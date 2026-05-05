package gg.cnmc.battlemanager.battle;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class BannerTracker {

    // Set when the banner is placed on blue concrete — this is the BANNER's actual block pos
    private static BlockPos bannerPos = null;
    private static BlockPos originalBannerPos = null;

    // Set when red concrete is placed — this is where attackers must return
    private static BlockPos attackerReturnPos = null;

    private static boolean bannerPickedUp = false;
    private static String bannerCarrier = null;
    private static boolean bannerAnnounced = false;

    public static void setBannerPos(BlockPos pos) {
        bannerPos = pos;
    }

    public static BlockPos getBannerPos() {
        return bannerPos;
    }

    public static boolean hasBannerPos() {
        return bannerPos != null;
    }

    public static void setAttackerReturnPos(BlockPos pos) {
        attackerReturnPos = pos;
    }

    public static BlockPos getAttackerReturnPos() {
        return attackerReturnPos;
    }

    public static boolean hasReturnPos() {
        return attackerReturnPos != null;
    }

    public static void setBannerPickedUp(boolean picked, String carrierName) {
        bannerPickedUp = picked;
        bannerCarrier = picked ? carrierName : null;
    }

    public static boolean isBannerPickedUp() {
        return bannerPickedUp;
    }

    public static String getBannerCarrier() {
        return bannerCarrier;
    }

    public static void setBannerAnnounced(boolean announced) {
        bannerAnnounced = announced;
    }

    public static boolean isBannerPlacedAndAnnounced() {
        return bannerAnnounced;
    }

    public static void setOriginalBannerPos(BlockPos pos) {
        originalBannerPos = pos;
    }

    public static void returnBanner(MinecraftServer server, ServerPlayerEntity carrier) {
        if (originalBannerPos == null) return;

        net.minecraft.block.entity.BannerBlockEntity bannerEntity = new net.minecraft.block.entity.BannerBlockEntity(
                originalBannerPos,
                net.minecraft.block.Blocks.BLUE_BANNER.getDefaultState()
        );
        bannerEntity.setCustomName(net.minecraft.text.Text.literal("§9§lBanner Objective").styled(s -> s.withItalic(false)));

        server.getOverworld().setBlockState(
                originalBannerPos,
                net.minecraft.block.Blocks.BLUE_BANNER.getDefaultState()
        );
        server.getOverworld().addBlockEntity(bannerEntity);

        setBannerPos(originalBannerPos);
        setBannerPickedUp(false, null);
    }

    public static void reset() {
        bannerPos = null;
        attackerReturnPos = null;
        bannerPickedUp = false;
        bannerCarrier = null;
        bannerAnnounced = false;
    }
}