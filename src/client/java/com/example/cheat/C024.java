package com.example.cheat;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.example.cheat.module.C017;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

/** Draws everything through walls with one custom pipeline (pattern from the Fabric 1.21.11 docs). */
public final class C024 {
  private static final String ID = "cheatclient";
  private static final RenderPipeline PIPE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
      .withLocation(Identifier.fromNamespaceAndPath(ID, "pipeline/through_walls"))
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .build());

  private static final ByteBufferBuilder ALLOC = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
  private static BufferBuilder buf;
  private static MappableRingBuffer vertexBuffer;
  private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
  private static final Vector3f MODEL_OFFSET = new Vector3f();
  private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

  private static Matrix4fc M; private static Vec3 CAM = Vec3.ZERO;
  private C024(){}
  public static Vec3 cam(){ return CAM; }

  /** Event entry point (WorldRenderEvents.BEFORE_TRANSLUCENT). */
  public static void render(WorldRenderContext ctx){
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null) return;
    CAM = ctx.worldState().cameraRenderState.pos;
    PoseStack ps = ctx.matrices();
    ps.pushPose(); ps.translate(-CAM.x, -CAM.y, -CAM.z); M = ps.last().pose();
    float td = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    for (C017 m : C001.MODS) if (m.enabled) m.onRender(mc, td);
    ps.popPose();
    drawAll(mc);
  }

  // ---------- geometry helpers (world coordinates) ----------
  private static BufferBuilder B(){
    if (buf == null) buf = new BufferBuilder(ALLOC, PIPE.getVertexFormatMode(), PIPE.getVertexFormat());
    return buf; }
  private static void vtx(Vec3 p, float r, float g, float b, float a){ B().addVertex(M, (float)p.x, (float)p.y, (float)p.z).setColor(r, g, b, a); }
  private static void quad(Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, int argb){
    float a=(argb>>>24&255)/255f, r=(argb>>16&255)/255f, g=(argb>>8&255)/255f, b=(argb&255)/255f;
    vtx(p1,r,g,b,a); vtx(p2,r,g,b,a); vtx(p3,r,g,b,a); vtx(p4,r,g,b,a);   // front
    vtx(p4,r,g,b,a); vtx(p3,r,g,b,a); vtx(p2,r,g,b,a); vtx(p1,r,g,b,a); } // back (in case of culling)

  /** Thin camera-facing strip. Width follows distance from the camera (constant on-screen thickness) and alpha can fade along the line. */
  private static final double LINE_PX = 0.003;   // thickness: raise for thicker lines, lower for thinner
  private static void ribbon(Vec3 a, Vec3 b, int rgb, float alphaA, float alphaB, double px){
    Vec3 d = b.subtract(a), mid = a.add(b).scale(0.5).subtract(CAM);
    Vec3 side = d.cross(mid); double len = side.length(); if (len < 1e-7) return;
    side = side.scale(1.0 / len);
    double wa = px * a.subtract(CAM).length() / 2.0, wb = px * b.subtract(CAM).length() / 2.0;
    Vec3 a1 = a.add(side.scale(wa)), a2 = a.subtract(side.scale(wa)), b1 = b.add(side.scale(wb)), b2 = b.subtract(side.scale(wb));
    float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, bl = (rgb & 255) / 255f;
    vtx(a1, r, g, bl, alphaA); vtx(a2, r, g, bl, alphaA); vtx(b2, r, g, bl, alphaB); vtx(b1, r, g, bl, alphaB);   // front
    vtx(b1, r, g, bl, alphaB); vtx(b2, r, g, bl, alphaB); vtx(a2, r, g, bl, alphaA); vtx(a1, r, g, bl, alphaA); } // back

  public static void box(AABB bb, int rgb, boolean fill){
    int fillc = 0x30000000 | (rgb & 0xFFFFFF);
    double[] X={bb.minX,bb.maxX}, Y={bb.minY,bb.maxY}, Z={bb.minZ,bb.maxZ};
    for (int i=0;i<2;i++) for (int j=0;j<2;j++) {
      ribbon(new Vec3(X[0],Y[i],Z[j]), new Vec3(X[1],Y[i],Z[j]), rgb, 1f, 1f, LINE_PX);
      ribbon(new Vec3(X[i],Y[0],Z[j]), new Vec3(X[i],Y[1],Z[j]), rgb, 1f, 1f, LINE_PX);
      ribbon(new Vec3(X[i],Y[j],Z[0]), new Vec3(X[i],Y[j],Z[1]), rgb, 1f, 1f, LINE_PX); }
    if (!fill) return;
    Vec3 a=new Vec3(X[0],Y[0],Z[0]), b=new Vec3(X[1],Y[0],Z[0]), c=new Vec3(X[1],Y[0],Z[1]), d=new Vec3(X[0],Y[0],Z[1]);
    Vec3 e=new Vec3(X[0],Y[1],Z[0]), f=new Vec3(X[1],Y[1],Z[0]), g=new Vec3(X[1],Y[1],Z[1]), h=new Vec3(X[0],Y[1],Z[1]);
    quad(a,b,c,d,fillc); quad(e,f,g,h,fillc); quad(a,b,f,e,fillc); quad(d,c,g,h,fillc); quad(a,d,h,e,fillc); quad(b,c,g,f,fillc); }

  /** Line from just in front of the crosshair to a world position. */
  public static void tracer(Minecraft mc, float td, Vec3 to, int rgb, double width, float opacity){
    Vec3 start = CAM.add(mc.player.getViewVector(td).scale(0.6));
    ribbon(start, to, rgb, opacity * 0.4f, opacity, width * 0.001); }   // width: 1 = thin, 15 = very thick

  public static AABB lerpBox(Entity e, float td){
    double x = Mth.lerp((double) td, e.xo, e.getX()), y = Mth.lerp((double) td, e.yo, e.getY()), z = Mth.lerp((double) td, e.zo, e.getZ());
    return e.getBoundingBox().move(x - e.getX(), y - e.getY(), z - e.getZ()); }

  // ---------- drawing (copied from the Fabric docs pattern) ----------
  private static void drawAll(Minecraft C001){
    if (buf == null) return;                       // nothing was added this frame
    MeshData built = buf.buildOrThrow(); buf = null;
    MeshData.DrawState dp = built.drawState(); VertexFormat format = dp.format();
    GpuBuffer vertices = upload(dp, format, built);
    draw(C001, PIPE, built, dp, vertices, format);
    vertexBuffer.rotate(); }

  private static GpuBuffer upload(MeshData.DrawState dp, VertexFormat format, MeshData built){
    int size = dp.vertexCount() * format.getVertexSize();
    if (vertexBuffer == null || vertexBuffer.size() < size) {
      if (vertexBuffer != null) vertexBuffer.close();
      vertexBuffer = new MappableRingBuffer(() -> ID + " vertex buffer", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size); }
    CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
    try (GpuBuffer.MappedView view = enc.mapBuffer(vertexBuffer.currentBuffer().slice(0, built.vertexBuffer().remaining()), false, true)) {
      MemoryUtil.memCopy(built.vertexBuffer(), view.data()); }
    return vertexBuffer.currentBuffer(); }

  private static void draw(Minecraft C001, RenderPipeline pipeline, MeshData built, MeshData.DrawState dp, GpuBuffer vertices, VertexFormat format){
    GpuBuffer indices; VertexFormat.IndexType indexType;
    if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
      built.sortQuads(ALLOC, RenderSystem.getProjectionType().vertexSorting());
      indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(built.indexBuffer());
      indexType = built.drawState().indexType();
    } else {
      RenderSystem.AutoStorageIndexBuffer sib = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
      indices = sib.getBuffer(dp.indexCount()); indexType = sib.type(); }
    GpuBufferSlice dyn = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
    try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> ID + " render pass",
        C001.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(), C001.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
      pass.setPipeline(pipeline);
      RenderSystem.bindDefaultUniforms(pass);
      pass.setUniform("DynamicTransforms", dyn);
      pass.setVertexBuffer(0, vertices);
      pass.setIndexBuffer(indices, indexType);
      pass.drawIndexed(0 / format.getVertexSize(), 0, dp.indexCount(), 1); }
    built.close(); }
}
