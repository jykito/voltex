package com.jykito.industrialcore.compat.emi;

import com.jykito.industrialcore.menu.MetalFormerMenu;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The click areas of the machine windows when EMI runs beside JEI. EMI shows the JEI categories but JEI's click areas do
 * nothing there, so a machine's progress would not lead to its recipes. This asks JEI which area is under the mouse,
 * shows the hint over it and opens its category in EMI. Started from the JEI plugin only when EMI is loaded, so the class
 * loads only with JEI present. EMI is reached by reflection: it is no build dependency of the mod.
 */
public final class JeiAreasUnderEmi {
    private static IJeiRuntime runtime;
    private static boolean registered;

    public static void start(IJeiRuntime r) {
        runtime = r;
        if (!registered) {
            registered = true;
            MinecraftForge.EVENT_BUS.register(JeiAreasUnderEmi.class);
        }
    }

    public static void stop() {
        runtime = null;
    }

    /** this mod's windows and those of its Nuclear addon, which registers its areas with JEI the same way */
    private static boolean served(Screen screen) {
        String name = screen.getClass().getName();
        return name.startsWith("com.jykito.industrialcore.") || name.startsWith("com.jykito.voltexnuclear.");
    }

    private static Optional<IGuiClickableArea> under(Screen screen, double mx, double my) {
        if (runtime == null || !(screen instanceof AbstractContainerScreen<?> c) || !served(screen) || !c.getMenu().getCarried().isEmpty())
            return Optional.empty();
        return runtime.getScreenHelper().getGuiClickableArea(c, mx - c.getGuiLeft(), my - c.getGuiTop()).findFirst();
    }

    /** the recipe types the area opens: its onClick is handed a stand-in for JEI's recipe window that only writes them down */
    private static List<RecipeType<?>> typesOf(IGuiClickableArea area) {
        List<RecipeType<?>> types = new ArrayList<>();
        IRecipesGui standIn = (IRecipesGui) Proxy.newProxyInstance(IRecipesGui.class.getClassLoader(), new Class<?>[]{IRecipesGui.class}, (proxy, method, args) -> {
            if (method.getName().equals("showTypes") && args[0] instanceof List<?> list)
                for (Object t : list) types.add((RecipeType<?>) t);
            return null;
        });
        area.onClick(runtime.getJeiHelpers().getFocusFactory(), standIn);
        return types;
    }

    /** opens the EMI category with that id; false when EMI has none (or its API has moved) */
    private static boolean show(ResourceLocation id) {
        try {
            Class<?> api = Class.forName("dev.emi.emi.api.EmiApi");
            Class<?> manager = Class.forName("dev.emi.emi.api.recipe.EmiRecipeManager");
            Class<?> category = Class.forName("dev.emi.emi.api.recipe.EmiRecipeCategory");
            for (Object c : (Iterable<?>) manager.getMethod("getCategories").invoke(api.getMethod("getRecipeManager").invoke(null)))
                if (id.equals(category.getMethod("getId").invoke(c))) {
                    api.getMethod("displayRecipeCategory", category).invoke(null, c);
                    return true;
                }
        } catch (ReflectiveOperationException e) {
            return false;
        }
        return false;
    }

    @SubscribeEvent
    public static void hint(ScreenEvent.Render.Post event) {
        int mx = event.getMouseX(), my = event.getMouseY();
        if (under(event.getScreen(), mx, my).isPresent())
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, Component.translatable("jei.tooltip.show.recipes"), mx, my);
    }

    @SubscribeEvent
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0) return;
        Screen screen = event.getScreen();
        under(screen, event.getMouseX(), event.getMouseY()).ifPresent(area -> {
            List<RecipeType<?>> types = typesOf(area);
            // EMI opens one category at a time: the metal former's is the one of its mode, otherwise the first that exists
            if (screen instanceof AbstractContainerScreen<?> c && c.getMenu() instanceof MetalFormerMenu former && former.getMode() < types.size()
                    && show(types.get(former.getMode()).getUid())) {
                event.setCanceled(true);
                return;
            }
            for (RecipeType<?> type : types)
                if (show(type.getUid())) {
                    event.setCanceled(true);
                    return;
                }
        });
    }

    private JeiAreasUnderEmi() {
    }
}
