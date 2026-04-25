package me.guivnf.mods.hats.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class CubeRenderer
{
    public static void renderCube(PoseStack poseStack, VertexConsumer consumer,
            float ox, float oy, float oz, float sx, float sy, float sz,
            float inflateX, float inflateY, float inflateZ,
            float texU, float texV, float dimX, float dimY, float dimZ,
            int texW, int texH,
            int light, int overlay, float r, float g, float b, float a,
            boolean mirror, boolean flatX, boolean flatY, boolean flatZ, boolean swapYFaceUV)
    {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        float dX = dimX;
        float dY = dimY;
        float dZ = dimZ;

        float iW = 1f / texW;
        float iH = 1f / texH;

        float u0 = texU;
        float v0 = texV;

        float x0 = ox - inflateX;
        float y0 = oy - inflateY;
        float z0 = oz - inflateZ;
        float x1 = ox + sx + inflateX;
        float y1 = oy + sy + inflateY;
        float z1 = oz + sz + inflateZ;

        float topU0    = (u0 + dZ)           * iW;
        float topU1    = (u0 + dZ + dX)      * iW;
        float topV0    = (v0)                 * iH;
        float topV1    = (v0 + dZ)            * iH;

        float bottomU0 = (u0 + dZ + dX)      * iW;
        float bottomU1 = (u0 + dZ + dX + dX) * iW;
        float bottomV0 = (v0)                 * iH;
        float bottomV1 = (v0 + dZ)            * iH;

        float frontU0  = (u0 + dZ)            * iW;
        float frontU1  = (u0 + dZ + dX)       * iW;
        float frontV0  = (v0 + dZ)            * iH;
        float frontV1  = (v0 + dZ + dY)       * iH;

        float backU0   = (u0 + dZ + dX + dZ)       * iW;
        float backU1   = (u0 + dZ + dX + dZ + dX)  * iW;
        float backV0   = (v0 + dZ)                  * iH;
        float backV1   = (v0 + dZ + dY)             * iH;

        float leftU0   = (u0 + dZ + dX)       * iW;
        float leftU1   = (u0 + dZ + dX + dZ)  * iW;
        float leftV0   = (v0 + dZ)             * iH;
        float leftV1   = (v0 + dZ + dY)        * iH;

        float rightU0  = (u0)       * iW;
        float rightU1  = (u0 + dZ)  * iW;
        float rightV0  = (v0 + dZ)  * iH;
        float rightV1  = (v0 + dZ + dY) * iH;

        if (mirror) {
            float tmp;
            tmp = rightU0;  rightU0  = rightU1;  rightU1  = tmp;
            tmp = leftU0;   leftU0   = leftU1;   leftU1   = tmp;
            tmp = frontU0;  frontU0  = frontU1;  frontU1  = tmp;
            tmp = backU0;   backU0   = backU1;   backU1   = tmp;
            tmp = topU0;    topU0    = topU1;    topU1    = tmp;
            tmp = bottomU0; bottomU0 = bottomU1; bottomU1 = tmp;
        }

        if (flatY) {
            if (swapYFaceUV) {
                // GEO_JSON flat Y: use topU/V with z1→z0 winding (matches original y0 face appearance)
                renderFace(pose, normal, consumer, x0, y1, z1, x0, y1, z0, x1, y1, z0, x1, y1, z1,
                    topU0, topV0, topU1, topV1, 0, 1, 0, light, overlay, r, g, b, a);
                renderFace(pose, normal, consumer, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1,
                    topU0, topV0, topU1, topV1, 0, -1, 0, light, overlay, r, g, b, a);
            } else {
                // TBL flat Y: use bottomU/V with z0→z1 winding
                renderFace(pose, normal, consumer, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0,
                    bottomU0, bottomV0, bottomU1, bottomV1, 0, 1, 0, light, overlay, r, g, b, a);
                renderFace(pose, normal, consumer, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0,
                    bottomU0, bottomV0, bottomU1, bottomV1, 0, -1, 0, light, overlay, r, g, b, a);
            }
        } else {
            renderFace(pose, normal, consumer, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0,
                bottomU0, bottomV0, bottomU1, bottomV1, 0, 1, 0, light, overlay, r, g, b, a);
            renderFace(pose, normal, consumer, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1,
                topU0, topV0, topU1, topV1, 0, -1, 0, light, overlay, r, g, b, a);
        }
        renderFace(pose, normal, consumer, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0,
            frontU0, frontV0, frontU1, frontV1, 0, 0, -1, light, overlay, r, g, b, a);
        if (flatZ)
            renderFace(pose, normal, consumer, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1,
                frontU0, frontV0, frontU1, frontV1, 0, 0, 1, light, overlay, r, g, b, a);
        else
            renderFace(pose, normal, consumer, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1,
                backU0, backV0, backU1, backV1, 0, 0, 1, light, overlay, r, g, b, a);
        renderFace(pose, normal, consumer, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0,
            rightU0, rightV0, rightU1, rightV1, -1, 0, 0, light, overlay, r, g, b, a);
        if (flatX)
            renderFace(pose, normal, consumer, x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0,
                rightU0, rightV0, rightU1, rightV1, 1, 0, 0, light, overlay, r, g, b, a);
        else
            renderFace(pose, normal, consumer, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1,
                leftU0, leftV0, leftU1, leftV1, 1, 0, 0, light, overlay, r, g, b, a);
    }

    private static void renderFace(Matrix4f pose, Matrix3f normal, VertexConsumer consumer,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz,
            float u0, float v0, float u1, float v1,
            float nx, float ny, float nz,
            int light, int overlay, float r, float g, float b, float a)
    {
        consumer.vertex(pose, ax, ay, az).color(r, g, b, a).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, bx, by, bz).color(r, g, b, a).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, cx, cy, cz).color(r, g, b, a).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, dx, dy, dz).color(r, g, b, a).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(normal, nx, ny, nz).endVertex();
    }
}
