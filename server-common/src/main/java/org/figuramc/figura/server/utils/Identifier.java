package org.figuramc.figura.server.utils;

/**
 * Identifier record made for compatibility
 * @param namespace Namespace of identifier
 * @param path Path of identifier
 */
public record Identifier(String namespace, String path) {
}