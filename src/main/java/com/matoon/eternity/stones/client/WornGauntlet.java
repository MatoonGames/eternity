package com.matoon.eternity.stones.client;

import com.google.common.collect.ImmutableMap;
import com.matoon.eternity.Eternity;
import com.matoon.eternity.stones.GauntletWear;
import lucraft.mods.lucraftcore.infinity.ModuleInfinity;
import lucraft.mods.lucraftcore.infinity.render.ItemModelInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.render.ItemRendererInfinityGauntlet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ForgeBlockStateV1;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The Infinity Gauntlet as snapping has worn it (see {@link GauntletWear}): its icon and its 3D model each drawn from
 * Lucraft's own, scorched, charred, cracked or fused, as painted by tools/GauntletWear.java.
 * <p>
 * Lucraft draws the gauntlet two ways, and both are wrapped here rather than replaced, so it goes on drawing the stones
 * set in it: the icon, a layered model, is baked again over each worn texture, and the gauntlet in the world, drawn by
 * its item renderer, is drawn with the worn texture standing in for its own.
 */
public final class WornGauntlet {

    public static final WornGauntlet INSTANCE = new WornGauntlet();

    private static final ResourceLocation[] ICONS = new ResourceLocation[GauntletWear.FUSED + 1];
    private static final ResourceLocation[] SKINS = new ResourceLocation[GauntletWear.FUSED + 1];
    private static final TextFormatting[] COLORS = {TextFormatting.GRAY, TextFormatting.GOLD, TextFormatting.RED, TextFormatting.DARK_RED, TextFormatting.DARK_GRAY};

    static {
        for (int stage = 1; stage <= GauntletWear.FUSED; stage++) {
            ICONS[stage] = new ResourceLocation(Eternity.MODID, "items/gauntlet_worn_" + stage);
            SKINS[stage] = new ResourceLocation(Eternity.MODID, "textures/models/gauntlet_worn_" + stage + ".png");
        }
    }

    private WornGauntlet() {
    }

    @SubscribeEvent
    public void onStitch(TextureStitchEvent.Pre event) {
        for (int stage = 1; stage <= GauntletWear.FUSED; stage++) {
            event.getMap().registerSprite(ICONS[stage]);
        }
    }

    /** After Lucraft has given the gauntlet its renderer. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onModelRegistry(ModelRegistryEvent event) {
        TileEntityItemStackRenderer lucraft = ModuleInfinity.INFINITY_GAUNTLET.getTileEntityItemStackRenderer();
        if (!(lucraft instanceof Renderer)) {
            ModuleInfinity.INFINITY_GAUNTLET.setTileEntityItemStackRenderer(new Renderer(lucraft));
        }
    }

    /** After Lucraft has baked the gauntlet's icon: the same model baked again over each worn texture. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onModelBake(ModelBakeEvent event) {
        IBakedModel lucraft = event.getModelRegistry().getObject(ItemModelInfinityGauntlet.LOCATION);
        if (lucraft == null || lucraft instanceof Icon) {
            return;
        }
        IModelState state = ForgeBlockStateV1.Transforms.get("forge:default-item").orElse(TRSRTransformation.identity());
        IBakedModel[] worn = new IBakedModel[GauntletWear.FUSED + 1];
        for (int stage = 1; stage <= GauntletWear.FUSED; stage++) {
            try {
                worn[stage] = ItemModelInfinityGauntlet.MODEL.retexture(ImmutableMap.of("base", ICONS[stage].toString()))
                        .bake(state, DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
            } catch (Exception e) {
                Eternity.LOG.warn("Could not bake the worn gauntlet, stage {}", stage, e);
            }
        }
        event.getModelRegistry().putObject(ItemModelInfinityGauntlet.LOCATION, new Icon(lucraft, worn));
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        int stage = GauntletWear.stage(event.getItemStack());
        if (stage > 0 && event.getItemStack().getItem() == ModuleInfinity.INFINITY_GAUNTLET) {
            event.getToolTip().add(1, COLORS[stage] + GauntletWear.name(stage) + (stage == GauntletWear.FUSED ? ": melted by the snap" : " by the snap"));
        }
    }

    // ------------------------------------------------------------------ the icon

    /** Lucraft's icon, handing a worn gauntlet to the same model baked over its worn texture. */
    private static final class Icon implements IBakedModel {
        private final IBakedModel lucraft;
        private final IBakedModel[] worn;
        private final ItemOverrideList overrides;

        Icon(IBakedModel lucraft, IBakedModel[] worn) {
            this.lucraft = lucraft;
            this.worn = worn;
            overrides = new ItemOverrideList(Collections.emptyList()) {
                @Override
                public IBakedModel handleItemState(IBakedModel model, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
                    IBakedModel base = worn[GauntletWear.stage(stack)];
                    if (base == null) {
                        base = lucraft;
                    }
                    // Lucraft's own overrides lay the stones set in it over whichever it is.
                    return base.getOverrides().handleItemState(base, stack, world, entity);
                }
            };
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            return lucraft.getQuads(state, side, rand);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return lucraft.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return lucraft.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return lucraft.isBuiltInRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return lucraft.getParticleTexture();
        }

        @SuppressWarnings("deprecation")
        @Override
        public ItemCameraTransforms getItemCameraTransforms() {
            return lucraft.getItemCameraTransforms();
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
            return lucraft.handlePerspective(type);
        }

        @Override
        public ItemOverrideList getOverrides() {
            return overrides;
        }
    }

    // ------------------------------------------------------------------ the gauntlet in the world

    /**
     * Lucraft's renderer, drawing a worn gauntlet. It binds its own texture by name; for the moment it draws, that name
     * is made to stand for the worn texture instead.
     */
    private static final class Renderer extends TileEntityItemStackRenderer {
        private final TileEntityItemStackRenderer lucraft;
        /** The texture manager's textures by name. */
        private Map<ResourceLocation, ITextureObject> loaded;

        Renderer(TileEntityItemStackRenderer lucraft) {
            this.lucraft = lucraft;
        }

        @Override
        public void renderByItem(ItemStack stack, float partialTicks) {
            int stage = GauntletWear.stage(stack);
            if (stage == 0) {
                lucraft.renderByItem(stack, partialTicks);
                return;
            }
            TextureManager textures = Minecraft.getMinecraft().getTextureManager();
            if (loaded == null) {
                loaded = ObfuscationReflectionHelper.getPrivateValue(TextureManager.class, textures, "mapTextureObjects", "field_110585_a");
            }
            ITextureObject worn = textures.getTexture(SKINS[stage]);
            if (worn == null) {
                worn = new SimpleTexture(SKINS[stage]);
                textures.loadTexture(SKINS[stage], worn);
            }
            ITextureObject own = loaded.put(ItemRendererInfinityGauntlet.TEXTURE, worn);
            try {
                lucraft.renderByItem(stack, partialTicks);
            } finally {
                if (own != null) {
                    loaded.put(ItemRendererInfinityGauntlet.TEXTURE, own);
                } else {
                    loaded.remove(ItemRendererInfinityGauntlet.TEXTURE);
                }
            }
        }
    }
}
