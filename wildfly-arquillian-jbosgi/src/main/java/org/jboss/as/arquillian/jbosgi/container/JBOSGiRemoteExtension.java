/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.jbosgi.container;

import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.as.arquillian.service.DependenciesProvider;
import org.jboss.modules.ModuleIdentifier;

/**
 * @author <a href="mailto:arcadiy@ivanov.biz">Arcadiy Ivanov</a>
 * @version $Revision: $
 */
public class JBOSGiRemoteExtension implements DependenciesProvider, RemoteLoadableExtension {

    @Override
    public Set<ModuleIdentifier> getDependencies() {
        final Set<ModuleIdentifier> archiveDependencies = new LinkedHashSet<ModuleIdentifier>();
        archiveDependencies.add(ModuleIdentifier.create("org.osgi.core"));
        return archiveDependencies;
    }

    @Override
    public void register(ExtensionBuilder builder) {
    }
}
