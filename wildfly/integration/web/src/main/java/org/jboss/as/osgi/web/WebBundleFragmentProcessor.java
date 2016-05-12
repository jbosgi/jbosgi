/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.osgi.web;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ee.structure.SpecDescriptorPropertyReplacement;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.OSGiLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.merge.web.jboss.JBossWebMetaDataMerger;
import org.jboss.metadata.parser.servlet.WebMetaDataParser;
import org.jboss.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.vfs.VirtualFile;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Process fragment attachments to a WAB.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 10-Dec-2012
 */
public class WebBundleFragmentProcessor implements DeploymentUnitProcessor {

    private final Map<XBundleRevision, VirtualFile> fragmentRoots = new HashMap<>();

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        XBundleRevision hostRev = depUnit.getAttachment(OSGiConstants.BUNDLE_REVISION_KEY);
        WarMetaData warMetaData = depUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);

        if (hostRev != null && hostRev.isFragment()) {
            ResourceRoot resourceRoot = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            if (resourceRoot != null) {
                synchronized (fragmentRoots) {
                    fragmentRoots.put(hostRev, resourceRoot.getRoot());
                }
            }
        }

        if (warMetaData == null || hostRev == null)
            return;

        List<XBundle> fragments = new ArrayList<XBundle>();

        // Get attached fragments
        BundleWiring wiring = hostRev.getWiring();
        for (BundleWire wire : wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE)) {
            fragments.add((XBundle) wire.getRequirer().getBundle());
            break;
        }

        // No attached fragments
        if (fragments.size() == 0)
            return;

        // Check if the first fragment has a web.xml entry
        XBundle fragment = fragments.get(0);
        URL entry = fragment.getEntry("WEB-INF/web.xml");
        if (entry != null) {
            // Parse the web.xml
            WebMetaData fragmentMetaData = null;
            try {
                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                MetaDataElementParser.DTDInfo dtdInfo = new MetaDataElementParser.DTDInfo();
                inputFactory.setXMLResolver(dtdInfo);
                XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(entry.openStream());
                fragmentMetaData = WebMetaDataParser.parse(xmlReader, dtdInfo, SpecDescriptorPropertyReplacement.propertyReplacer(depUnit));
            } catch (XMLStreamException ex) {
                OSGiLogger.LOGGER.errorf(ex, "Cannot parse web.xml in fragment: %s", fragment);
            } catch (IOException ex) {
                OSGiLogger.LOGGER.errorf(ex, "Cannot parse web.xml in fragment: %s", fragment);
            }
            // Merge additional {@link WebMetaData}
            if (fragmentMetaData != null) {
                warMetaData.setWebMetaData(fragmentMetaData);
                JBossWebMetaData mergedMetaData = new JBossWebMetaData();
                JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();
                JBossWebMetaDataMerger.merge(mergedMetaData, metaData, fragmentMetaData);
                warMetaData.setMergedJBossWebMetaData(mergedMetaData);
            }
        }

        Set<VirtualFile> overlays = new LinkedHashSet<>();
        synchronized (fragmentRoots) {
            for (XBundle frag : fragments) {
                VirtualFile fragRoot = fragmentRoots.get(frag.getBundleRevision());
                if (fragRoot != null) {
                    overlays.add(fragRoot);
                }
            }
        }
        if (overlays.size() > 0) {
            warMetaData.setOverlays(overlays);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        XBundleRevision hostRev = depUnit.getAttachment(OSGiConstants.BUNDLE_REVISION_KEY);
        if (hostRev != null && hostRev.isFragment()) {
            synchronized (fragmentRoots) {
                fragmentRoots.remove(hostRev);
            }
        }
    }
}
