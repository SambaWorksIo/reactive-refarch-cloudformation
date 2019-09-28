/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package com.amazon;

import com.amazon.verticles.CacheVerticle;
import com.amazon.verticles.HttpVerticle;
import com.amazon.verticles.KinesisVerticle;
import com.amazon.verticles.RedisVerticle;
import io.vertx.core.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.getenv;

public class BootStrapVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootStrapVerticle.class);

    static {
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");
    }

    public static void main(String... args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new BootStrapVerticle());
    }

    @Override
    public void start(Future<Void> startFuture) {

        String trustStoreLocation = getenv("javax.net.ssl.trustStore");
        String trustAnchorsLocation = getenv("javax.net.ssl.trustAnchors");

        if (null != trustStoreLocation) {
            LOGGER.info("Setting javax.net.ssl.trustStore to " + trustStoreLocation);
            System.setProperty("javax.net.ssl.trustStore", trustStoreLocation);
        } else {
            LOGGER.info("Setting javax.net.ssl.trustStore not set");
        }

        if (null != trustAnchorsLocation) {
            LOGGER.info("Setting javax.net.ssl.trustAnchors to " + trustAnchorsLocation);
            System.setProperty("javax.net.ssl.trustAnchors", trustAnchorsLocation);
        } else {
            LOGGER.info("Setting javax.net.ssl.trustAnchors not set");
        }

        List<Future> futures = Stream.generate(Future::<String>future).limit(4)
                .collect(Collectors.toList());

        LOGGER.info("Deploying RedisVerticle");
        this.deployVerticle(RedisVerticle::new, new DeploymentOptions().setInstances(1), futures.get(0));

        LOGGER.info("Deploying CacheVerticle");
        this.deployVerticle(CacheVerticle::new, new DeploymentOptions().setInstances(1), futures.get(1));

        LOGGER.info("Deploying HttpVerticle");
        this.deployVerticle(HttpVerticle::new, new DeploymentOptions().setInstances(5), futures.get(2));

        LOGGER.info("Deploying KinesisVerticle");
        this.deployVerticle(KinesisVerticle::new, new DeploymentOptions().setInstances(5), futures.get(3));

        CompositeFuture.all(futures).setHandler(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(ar.cause());
            }
        });
    }

    private void deployVerticle(final Supplier<Verticle> verticleSupplier, final DeploymentOptions deploymentOptions,
                                Future<String> future) {
        vertx.deployVerticle(verticleSupplier, deploymentOptions, deployment ->
        {
            if (!deployment.succeeded()) {
                LOGGER.error(deployment.cause());
                future.fail(deployment.cause());
            } else {
                future.complete();
            }
        });
    }
}
