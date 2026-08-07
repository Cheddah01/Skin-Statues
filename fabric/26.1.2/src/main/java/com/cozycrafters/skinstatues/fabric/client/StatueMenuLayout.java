package com.cozycrafters.skinstatues.fabric.client;

/** Pure logical-pixel layout for the compact statue menu. */
public record StatueMenuLayout(
        Bounds panel,
        Bounds playerField,
        Bounds scaleField,
        Bounds createButton,
        Bounds undoButton,
        int titleY,
        int subtitleY,
        int playerLabelY,
        int scaleLabelY,
        int statusY,
        int statusHeight,
        boolean showSubtitle) {
    public static final int PREFERRED_PANEL_WIDTH = 300;
    public static final int NORMAL_PANEL_HEIGHT = 212;
    public static final int COMPACT_PANEL_HEIGHT = 156;
    public static final int WIDGET_HEIGHT = 20;
    public static final int STATUS_HEIGHT = 18;

    private static final int SCREEN_MARGIN_X = 12;
    private static final int SCREEN_MARGIN_Y = 2;
    private static final int NORMAL_HEIGHT_THRESHOLD = 300;

    public static StatueMenuLayout calculate(int screenWidth, int screenHeight) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        boolean compact = safeHeight < NORMAL_HEIGHT_THRESHOLD;
        int desiredPanelHeight = compact ? COMPACT_PANEL_HEIGHT : NORMAL_PANEL_HEIGHT;
        int panelHeight = Math.min(desiredPanelHeight, Math.max(1, safeHeight - SCREEN_MARGIN_Y * 2));
        int panelWidth = Math.min(PREFERRED_PANEL_WIDTH, Math.max(1, safeWidth - SCREEN_MARGIN_X * 2));
        int panelX = (safeWidth - panelWidth) / 2;
        int panelY = (safeHeight - panelHeight) / 2;
        int contentPadding = panelWidth >= 260 ? 16 : 10;
        int contentX = panelX + contentPadding;
        int contentWidth = Math.max(1, panelWidth - contentPadding * 2);

        if (compact) {
            return new StatueMenuLayout(
                    new Bounds(panelX, panelY, panelWidth, panelHeight),
                    new Bounds(contentX, panelY + 29, contentWidth, WIDGET_HEIGHT),
                    new Bounds(contentX, panelY + 62, contentWidth, WIDGET_HEIGHT),
                    new Bounds(contentX, panelY + 108, contentWidth, WIDGET_HEIGHT),
                    new Bounds(contentX, panelY + 132, contentWidth, WIDGET_HEIGHT),
                    panelY + 5,
                    panelY + 5,
                    panelY + 19,
                    panelY + 52,
                    panelY + 85,
                    STATUS_HEIGHT,
                    false);
        }

        return new StatueMenuLayout(
                new Bounds(panelX, panelY, panelWidth, panelHeight),
                new Bounds(contentX, panelY + 60, contentWidth, WIDGET_HEIGHT),
                new Bounds(contentX, panelY + 99, contentWidth, WIDGET_HEIGHT),
                new Bounds(contentX, panelY + 151, contentWidth, WIDGET_HEIGHT),
                new Bounds(contentX, panelY + 177, contentWidth, WIDGET_HEIGHT),
                panelY + 12,
                panelY + 27,
                panelY + 49,
                panelY + 88,
                panelY + 126,
                STATUS_HEIGHT,
                true);
    }

    public record Bounds(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean contains(Bounds other) {
            return other.x >= x && other.y >= y && other.right() <= right() && other.bottom() <= bottom();
        }
    }
}
