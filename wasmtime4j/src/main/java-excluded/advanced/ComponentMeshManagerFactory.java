/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;

/**
 * Factory for creating ComponentMeshManager instances.
 *
 * <p>This factory handles the runtime detection and creates the appropriate
 * implementation (JNI or Panama) based on the Java version and configuration.
 *
 * @since 1.0.0
 */
final class ComponentMeshManagerFactory {
    private static final Logger logger = Logger.getLogger(ComponentMeshManagerFactory.class.getName());

    private ComponentMeshManagerFactory() {
        // Utility class
    }

    /**
     * Create a new component mesh manager with default configuration.
     *
     * @return new component mesh manager
     * @throws WasmRuntimeException if creation fails
     */
    static ComponentMeshManager create() {
        return create(MeshConfig.defaultConfig());
    }

    /**
     * Create a new component mesh manager with specified configuration.
     *
     * @param config the mesh configuration
     * @return new component mesh manager
     * @throws WasmRuntimeException if creation fails
     */
    static ComponentMeshManager create(final MeshConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Mesh configuration cannot be null");
        }

        final WasmRuntimeFactory.RuntimeType runtimeType = WasmRuntimeFactory.detectRuntime();
        logger.fine("Creating ComponentMeshManager with runtime: " + runtimeType);

        try {
            switch (runtimeType) {
                case PANAMA:
                    return createPanamaManager(config);
                case JNI:
                    return createJniManager(config);
                default:
                    throw new WasmRuntimeException("Unsupported runtime type: " + runtimeType);
            }
        } catch (final Exception e) {
            throw new WasmRuntimeException("Failed to create ComponentMeshManager", e);
        }
    }

    private static ComponentMeshManager createPanamaManager(final MeshConfig config) {
        try {
            final Class<?> panamaManagerClass = Class.forName(
                "ai.tegmentum.wasmtime4j.panama.PanamaComponentMeshManagerImpl");
            final Object panamaManager = panamaManagerClass.getDeclaredConstructor(MeshConfig.class)
                .newInstance(config);
            return (ComponentMeshManager) panamaManager;
        } catch (final Exception e) {
            logger.warning("Failed to create Panama ComponentMeshManager: " + e.getMessage());
            logger.info("Falling back to JNI implementation");
            return createJniManager(config);
        }
    }

    private static ComponentMeshManager createJniManager(final MeshConfig config) {
        try {
            final Class<?> jniManagerClass = Class.forName(
                "ai.tegmentum.wasmtime4j.jni.JniComponentMeshManagerImpl");
            final Object jniManager = jniManagerClass.getDeclaredConstructor(MeshConfig.class)
                .newInstance(config);
            return (ComponentMeshManager) jniManager;
        } catch (final Exception e) {
            throw new WasmRuntimeException("Failed to create JNI ComponentMeshManager", e);
        }
    }
}