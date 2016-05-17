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

package org.jboss.as.osgi.deployment;

import static org.jboss.osgi.framework.spi.IntegrationConstants.BUNDLE_INFO_KEY;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.service.BundleLifecycleIntegration;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.JPADeploymentMarker;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.framework.spi.IntegrationConstants;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.spi.AttachmentKey;
import org.jboss.osgi.spi.BundleInfo;

/**
 * Processes deployments that have OSGi metadata attached.
 *
 * If so, it creates an {@link Deployment}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class BundleDeploymentProcessor implements DeploymentUnitProcessor {

    public static final AttachmentKey<DeploymentUnit> DEPLOYMENT_UNIT_KEY = AttachmentKey.create(DeploymentUnit.class);
    public static final AttachmentKey<ModuleSpecification> MODULE_SPECIFICATION_KEY = AttachmentKey.create(ModuleSpecification.class);

    public final String [] EXCLUDED_SUBSYSTEMS = {"batch"};

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final String runtimeName = depUnit.getName();

        // Check if {@link BundleLifecycleIntegration} provided the {@link Deployment}
        Deployment deployment = BundleLifecycleIntegration.removeDeployment(runtimeName);
        if (deployment != null) {
            deployment.setAutoStart(false);
        }

        // Check for attached BundleInfo
        BundleInfo info = depUnit.getAttachment(OSGiConstants.BUNDLE_INFO_KEY);
        if (info != null) {
            OSGiMetaData metadata = info.getOSGiMetadata();

            if (deployment == null) {
                deployment = DeploymentFactory.createDeployment(info);
                deployment.putAttachment(BUNDLE_INFO_KEY, info);
                deployment.setAutoStart(!metadata.isFragment());

                // Set the start level and prevent autostart if greater than the Framwork startlevel
                AnnotationInstance slAware = getAnnotation(depUnit, "org.jboss.arquillian.osgi.StartLevelAware");
                if (slAware != null) {
                    MethodInfo slTarget = (MethodInfo) slAware.target();
                    for (AnnotationInstance anDeployment : getAnnotations(depUnit, "org.jboss.arquillian.container.test.api.Deployment")) {
                        AnnotationValue namevalue = anDeployment.value("name");
                        Object deploymentName = namevalue != null ? namevalue.value() : null;
                        if (slTarget == anDeployment.target() && depUnit.getName().equals(deploymentName)) {
                            int startLevel = slAware.value("startLevel").asInt();
                            deployment.setStartLevel(startLevel);
                            deployment.setAutoStart(false);
                        }
                    }
                }

                // Prevent autostart for marked deployments
                AnnotationInstance marker = getAnnotation(depUnit, "org.jboss.as.arquillian.jbosgi.api.DeploymentMarker");
                if (marker != null) {
                    AnnotationValue value = marker.value("autoStart");
                    if (value != null && deployment.isAutoStart()) {
                        deployment.setAutoStart(value.asBoolean());
                    }
                    value = marker.value("startLevel");
                    if (value != null && deployment.getStartLevel() == null) {
                        deployment.setStartLevel(value.asInt());
                    }
                }
            }
        }

        // Attach the deployment
        if (deployment != null) {

            // Make sure the framework uses the same module id as the server
            ModuleIdentifier identifier = depUnit.getAttachment(Attachments.MODULE_IDENTIFIER);
            deployment.putAttachment(IntegrationConstants.MODULE_IDENTIFIER_KEY, identifier);

            // Allow additional dependencies for the set of supported deployment types
            if (allowAdditionalModuleDependencies(depUnit)) {
                ModuleSpecification moduleSpec = depUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
                deployment.putAttachment(BundleDeploymentProcessor.MODULE_SPECIFICATION_KEY, moduleSpec);
            } else {
                // Make this module private so that other modules in the deployment don't create a direct dependency
                ModuleSpecification moduleSpec = depUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
                moduleSpec.setPrivateModule(true);
            }

            // Attach the bundle deployment
            depUnit.putAttachment(OSGiConstants.DEPLOYMENT_KEY, deployment);
            deployment.putAttachment(BundleDeploymentProcessor.DEPLOYMENT_UNIT_KEY, depUnit);
            final DeploymentUnit parent;
            if (depUnit.getParent() == null) {
                parent = depUnit;
            } else {
                parent = depUnit.getParent();
            }

            boolean noExistingSubsystems = false;
            Set<String> excludedSubsystems = depUnit.getAttachment(Attachments.EXCLUDED_SUBSYSTEMS);
            if (excludedSubsystems == null) {
                excludedSubsystems = new HashSet<String>();
                noExistingSubsystems = true;
            }
            excludedSubsystems.addAll(Arrays.asList(EXCLUDED_SUBSYSTEMS));

            OSGiMetaData metadata = depUnit.getAttachment(OSGiConstants.OSGI_METADATA_KEY);
            if (metadata != null && metadata.isFragment()) {
                // JBOSGI-751, JBOSGI-761, JBOSGI-793, JBOSGI-794
                excludedSubsystems.add("webservices");
                excludedSubsystems.add("ee");
                excludedSubsystems.add("ejb3");
            }

            if (noExistingSubsystems) {
                depUnit.putAttachment(Attachments.EXCLUDED_SUBSYSTEMS, excludedSubsystems);
            }

            parent.putAttachment(Attachments.ALLOW_PHASE_RESTART, true);
        }
    }

    private List<AnnotationInstance> getAnnotations(DeploymentUnit depUnit, String className) {
        CompositeIndex index = depUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        List<AnnotationInstance> annotations = index.getAnnotations(DotName.createSimple(className));
        return annotations;
    }

    private AnnotationInstance getAnnotation(DeploymentUnit depUnit, String className) {
        List<AnnotationInstance> annotations = getAnnotations(depUnit, className);
        return annotations.size() == 1 ? annotations.get(0) : null;
    }

    private boolean allowAdditionalModuleDependencies(final DeploymentUnit depUnit) {
        boolean isWar = DeploymentTypeMarker.isType(DeploymentType.WAR, depUnit);
        boolean isEjb = EjbDeploymentMarker.isEjbDeployment(depUnit);
        boolean isCDI = WeldDeploymentMarker.isPartOfWeldDeployment(depUnit);
        boolean isJPA = JPADeploymentMarker.isJPADeployment(depUnit);
        return isWar || isEjb || isCDI || isJPA;
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        depUnit.removeAttachment(OSGiConstants.DEPLOYMENT_KEY);
    }
}
