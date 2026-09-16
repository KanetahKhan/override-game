package com.override.chapter1;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Loader for the compact {@code .mesh} resources under {@code /assets}.
 *
 * <p>JavaFX has no model importer, so authored models are converted offline by
 * {@code tools/convert.py} (FBX in, {@code .mesh} out) and only the finished
 * vertex arrays ship with the game. Triangulation, transform baking and scaling
 * all happen in that script, which leaves this side a straight array read.</p>
 *
 * <p>Layout, big-endian throughout: the ASCII magic {@code OVMESH01}, then the
 * point, texture-coordinate and triangle counts, then points (3 floats each),
 * normals (3 floats each, one per point), texture coordinates (2 floats each)
 * and finally faces — nine ints per triangle, as
 * {@code point, normal, texCoord} per corner.</p>
 */
final class MeshAsset {

    private static final String MAGIC = "OVMESH01";

    private MeshAsset() { }

    /** @throws IOException if the resource is absent, truncated, or not a mesh */
    static TriangleMesh load(String resource) throws IOException {
        try (InputStream in = MeshAsset.class.getResourceAsStream(resource)) {
            if (in == null) throw new IOException("not on the classpath: " + resource);
            ByteBuffer buf = ByteBuffer.wrap(in.readAllBytes());
            if (buf.remaining() < 20) throw new IOException("truncated: " + resource);

            byte[] magic = new byte[8];
            buf.get(magic);
            if (!MAGIC.equals(new String(magic, StandardCharsets.US_ASCII))) {
                throw new IOException("not an Override mesh: " + resource);
            }
            int points = buf.getInt(), texCoords = buf.getInt(), triangles = buf.getInt();
            if (points <= 0 || texCoords <= 0 || triangles <= 0) {
                throw new IOException("empty mesh: " + resource);
            }

            TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
            mesh.getPoints().setAll(floats(buf, points * 3, resource));
            mesh.getNormals().setAll(floats(buf, points * 3, resource));
            mesh.getTexCoords().setAll(floats(buf, texCoords * 2, resource));
            mesh.getFaces().setAll(ints(buf, triangles * 9, resource));
            return mesh;
        }
    }

    private static float[] floats(ByteBuffer buf, int n, String resource) throws IOException {
        if (buf.remaining() < n * 4) throw new IOException("truncated: " + resource);
        float[] out = new float[n];
        buf.asFloatBuffer().get(out);
        buf.position(buf.position() + n * 4);
        return out;
    }

    private static int[] ints(ByteBuffer buf, int n, String resource) throws IOException {
        if (buf.remaining() < n * 4) throw new IOException("truncated: " + resource);
        int[] out = new int[n];
        buf.asIntBuffer().get(out);
        buf.position(buf.position() + n * 4);
        return out;
    }
}
