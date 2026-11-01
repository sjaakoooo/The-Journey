package com.thejourney.client;

import com.thejourney.client.recap.Anim;
import com.thejourney.client.recap.Fx;
import com.thejourney.client.recap.Gallery;
import com.thejourney.client.recap.RecapModel;
import com.thejourney.client.recap.RecapView;
import com.thejourney.client.recap.Sfx;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The stand-alone recap (opened with the J key). It shows exactly the same experience as the
 * End Credits menu, centred on the screen. The data model and the interface live in the
 * {@code recap} package; this class only hosts them.
 */
public class JourneyScreen extends Screen {
    private final Screen parent;
    private final RecapView view;

    private boolean opened = false;
    private long openedAt = 0;
    private long closeStart = -1;

    public JourneyScreen() {
        this(null);
    }

    public JourneyScreen(Screen parent) {
        super(Component.literal("The Journey"));
        this.parent = parent;
        this.view = new RecapView(new RecapView.Host() {
            @Override
            public boolean standalone() {
                return true;
            }

            @Override
            public void done() {
                beginClose();
            }
        });
    }

    /** Called when the server sends (or we ask for) the recap text. */
    public static void setRecapText(String text) {
        RecapModel.set(text);
    }

    /** Kept for the (inactive) scroll mixin: scrolls the credits recap menu. */
    public static void scrollCreditCards(double verticalAmount) {
        CreditsMenu.scrollBy(verticalAmount);
    }

    @Override
    protected void init() {
        if (!opened) {
            opened = true;
            openedAt = Anim.now();
            view.open();
            Sfx.play(Sfx.Kind.OPEN);
            Sfx.startMusic();
        }
    }

    private void beginClose() {
        if (closeStart >= 0) {
            return;
        }
        if (!Anim.on()) {
            finishClose();
            return;
        }
        closeStart = Anim.now();
        Sfx.play(Sfx.Kind.CLOSE);
    }

    private void finishClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        beginClose();
    }

    @Override
    public void removed() {
        Sfx.stopMusic();
        Gallery.release();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fillGradient(0, 0, this.width, this.height, 0xD0080610, 0xF0030208);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        float open = Anim.prog(openedAt, 0, 380);
        float fade = 1.0F;
        if (closeStart >= 0) {
            float c = Anim.prog(closeStart, 0, 320);
            fade = 1.0F - c;
            if (c >= 1.0F) {
                finishClose();
                return;
            }
        }

        int w = Math.min(540, this.width - 20);
        int h = this.height - 20;
        int x = (this.width - w) / 2;
        int y = 10;
        int lift = Math.round((1.0F - Anim.outCubic(open)) * 14.0F);

        view.render(graphics, x, y + lift, w, h, mouseX, mouseY, Math.min(open, fade));
        Fx.alpha = 1.0F;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closeStart < 0 && view.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (view.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        view.scrollBy(verticalAmount);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            view.step(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            view.step(-1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
