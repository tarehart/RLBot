package tarehart.rlbot.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import tarehart.rlbot.Bot;

import javax.swing.*;
import java.awt.*;

public class StatusLineItem extends JPanel {
    private JLabel botDescription;
    private JButton detailsButton;
    private final JFrame detailsPanel;
    private int playerIndex;

    public StatusLineItem(Bot.Team team, int playerIndex, JFrame detailsPanel) {

        setupUI();

        this.detailsPanel = detailsPanel;
        this.playerIndex = playerIndex;
        botDescription.setText("Player " + playerIndex + " - " + team.name().toLowerCase());
        this.setBackground(team == Bot.Team.BLUE ? new Color(187, 212, 255) : new Color(250, 222, 191));
        detailsButton.addActionListener(e -> showDebugForm());
    }

    public void showDebugForm() {
        detailsPanel.pack();
        detailsPanel.setVisible(true);
    }

    private void setupUI() {
        this.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        this.setEnabled(false);
        botDescription = new JLabel();
        botDescription.setText("Label");
        this.add(botDescription, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(200, -1), new Dimension(200, 16), null, 0, false));
        detailsButton = new JButton();
        detailsButton.setText("Details");
        this.add(detailsButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    public int getPlayerIndex() {
        return playerIndex;
    }
}