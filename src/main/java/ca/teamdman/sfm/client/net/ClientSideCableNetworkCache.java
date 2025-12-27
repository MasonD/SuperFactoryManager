package ca.teamdman.sfm.client.net;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Set;
import java.util.WeakHashMap;

@SideOnly(Side.CLIENT)
public class ClientSideCableNetworkCache {
    WeakHashMap<World, Set<Set<BlockPos>>> cableNetworksByLevel = new WeakHashMap<>();
}
