package ca.teamdman.sfm.common.compat;

import appeng.api.AEPlugin;
import appeng.api.IAppEngApi;
import appeng.api.definitions.IBlockDefinition;
import appeng.api.definitions.IItemDefinition;
import appeng.api.definitions.IParts;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHelper;
import appeng.api.parts.PartItemStack;
import ca.teamdman.sfm.common.registry.SFMResourceTypes;
import ca.teamdman.sfm.common.resourcetype.ResourceTypeContainer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Set;

@AEPlugin
public class SFMAE2Compat {
    private static SFMAE2Compat INSTANCE;

    public final IAppEngApi api;
    public final IPartHelper partHelper;

    public final IParts parts;
    public final IItemDefinition[] fluidParts;
    public final IItemDefinition[] itemParts;

    public SFMAE2Compat(IAppEngApi api) {
        this.api = api;
        this.partHelper = api.partHelper();
        parts = api.definitions().parts();

        fluidParts = new IItemDefinition[] {
                parts.p2PTunnelFluids(),
                parts.fluidIface(),
        };

        itemParts = new IItemDefinition[] {
                parts.p2PTunnelItems(),
                parts.iface()
        };

        INSTANCE = this;
    }

    public static SFMAE2Compat instance() {
        return INSTANCE;
    }

    public boolean isAE2Cable(World level, BlockPos pos) {
        return api.definitions().blocks().multiPart().isSameAs(level, pos);

    }

    public boolean doesPartSupportResourceType(IPart part, ResourceTypeContainer.ResourceType<?,?,?>resourceType) {
        var stack = part.getItemStack(PartItemStack.WORLD);
        if (resourceType == SFMResourceTypes.ITEM) {
            for (IItemDefinition itemPart : itemParts) {
                if (itemPart.isSameAs(stack)) return true;
            }
            return false;
        }
        if (resourceType == SFMResourceTypes.FLUID) {
            for (IItemDefinition fluidPart : fluidParts) {
                if (fluidPart.isSameAs(stack)) return true;
            }
            return false;
        }
    }
}
