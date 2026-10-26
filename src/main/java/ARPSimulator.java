
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ARPSimulator extends JFrame {

    private final ARPManager manager = new ARPManager();

    private final Color background = new Color(17, 24, 39);
    private final Color panelColor = new Color(31, 41, 55);
    private final Color accent = new Color(56, 189, 248);
    private final Color textColor = new Color(243, 244, 246);
    private final Color mutedColor = new Color(156, 163, 175);

    private JTable deviceTable;
    private JTable cacheTable;

    private DefaultTableModel deviceModel;
    private DefaultTableModel cacheModel;

    private JComboBox<NetworkDevice> senderCombo;
    private JComboBox<String> targetCombo;
    private JComboBox<NetworkDevice> cacheDeviceCombo;

    private JSpinner ttlSpinner;

    private JTextArea eventLogArea;

    private JLabel requestLabel;
    private JLabel replyLabel;
    private JLabel hitLabel;
    private JLabel missLabel;
    private JLabel failureLabel;
    private JLabel statusLabel;

    public ARPSimulator() {
        setTitle("ARP Request-Reply & Cache Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);

        getContentPane().setBackground(background);
        setLayout(new BorderLayout(12, 12));

        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setBackground(background);
        center.setBorder(new EmptyBorder(0, 16, 0, 16));

        center.add(createStatisticsPanel(), BorderLayout.NORTH);
        center.add(createMainPanel(), BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
        add(createEventLogPanel(), BorderLayout.SOUTH);

        refreshAll();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(background);
        header.setBorder(new EmptyBorder(16, 20, 8, 20));

        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(background);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("ARP NETWORK SIMULATOR");
        title.setForeground(accent);
        title.setFont(new Font("SansSerif", Font.BOLD, 23));

        JLabel subtitle = new JLabel(
            "Address Resolution Protocol | Request, Reply & Cache"
        );
        subtitle.setForeground(mutedColor);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitle);

        statusLabel = new JLabel("● Simulation Ready");
        statusLabel.setForeground(new Color(74, 222, 128));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        header.add(titlePanel, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);

        return header;
    }

    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 10, 0));
        panel.setBackground(background);
        panel.setPreferredSize(new Dimension(1000, 85));

        requestLabel = new JLabel("0");
        replyLabel = new JLabel("0");
        hitLabel = new JLabel("0");
        missLabel = new JLabel("0");
        failureLabel = new JLabel("0");

        panel.add(createStatCard("ARP REQUESTS", requestLabel));
        panel.add(createStatCard("ARP REPLIES", replyLabel));
        panel.add(createStatCard("CACHE HITS", hitLabel));
        panel.add(createStatCard("CACHE MISSES", missLabel));
        panel.add(createStatCard("FAILED RESOLUTIONS", failureLabel));

        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel();
        card.setBackground(panelColor);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(55, 65, 81)),
            new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(mutedColor);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));

        valueLabel.setForeground(accent);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 25));

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);

        return card;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 0));
        panel.setBackground(background);

        panel.add(createDevicePanel());
        panel.add(createARPPanel());

        return panel;
    }

    private JPanel createDevicePanel() {
        JPanel panel = createSectionPanel("NETWORK DEVICES");

        deviceModel = new DefaultTableModel(
            new String[]{"Name", "IP Address", "MAC Address"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        deviceTable = new JTable(deviceModel);
        styleTable(deviceTable);

        JScrollPane scrollPane = new JScrollPane(deviceTable);
        styleScrollPane(scrollPane);

        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 3, 6, 0));
        buttons.setBackground(panelColor);
        buttons.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton addButton = createButton("＋ Add");
        JButton editButton = createButton("✎ Edit");
        JButton removeButton = createButton("− Remove");

        addButton.addActionListener(e -> addDevice());
        editButton.addActionListener(e -> editDevice());
        removeButton.addActionListener(e -> removeDevice());

        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(removeButton);

        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createARPPanel() {
        JPanel panel = createSectionPanel("ARP RESOLUTION & CACHE");

        JPanel content = new JPanel();
        content.setBackground(panelColor);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        senderCombo = new JComboBox<>();
        targetCombo = new JComboBox<>();
        cacheDeviceCombo = new JComboBox<>();

        ttlSpinner = new JSpinner(
            new SpinnerNumberModel(60, 5, 3600, 5)
        );

        content.add(createFieldLabel("Sender Device"));
        content.add(senderCombo);
        content.add(Box.createVerticalStrut(8));

        content.add(createFieldLabel("Target IP Address"));
        content.add(targetCombo);
        content.add(Box.createVerticalStrut(10));

        JButton resolveButton = createButton("▶  Send ARP Request");
        resolveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        resolveButton.addActionListener(e -> resolveARP());

        content.add(resolveButton);
        content.add(Box.createVerticalStrut(8));

        JPanel ttlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ttlPanel.setBackground(panelColor);

        JLabel ttlLabel = new JLabel("Cache TTL (seconds): ");
        ttlLabel.setForeground(textColor);

        ttlPanel.add(ttlLabel);
        ttlPanel.add(ttlSpinner);

        JButton applyTTLButton = createButton("Apply TTL");
        applyTTLButton.addActionListener(e -> applyTTL());

        ttlPanel.add(Box.createHorizontalStrut(8));
        ttlPanel.add(applyTTLButton);

        content.add(ttlPanel);
        content.add(Box.createVerticalStrut(10));

        JLabel cacheTitle = createFieldLabel("ARP CACHE");
        content.add(cacheTitle);

        JPanel cacheSelector = new JPanel(new BorderLayout(8, 0));
        cacheSelector.setBackground(panelColor);

        JLabel cacheDeviceLabel = new JLabel("View cache for:");
        cacheDeviceLabel.setForeground(textColor);

        cacheSelector.add(cacheDeviceLabel, BorderLayout.WEST);
        cacheSelector.add(cacheDeviceCombo, BorderLayout.CENTER);

        cacheDeviceCombo.addActionListener(e -> refreshCacheTable());

        content.add(cacheSelector);
        content.add(Box.createVerticalStrut(5));

        cacheModel = new DefaultTableModel(
            new String[]{"IP Address", "MAC Address", "Age (s)"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        cacheTable = new JTable(cacheModel);
        styleTable(cacheTable);

        JScrollPane cacheScroll = new JScrollPane(cacheTable);
        styleScrollPane(cacheScroll);
        cacheScroll.setPreferredSize(new Dimension(400, 130));
        cacheScroll.setMinimumSize(new Dimension(250, 100));

        content.add(cacheScroll);
        content.add(Box.createVerticalStrut(8));

        JPanel actionButtons = new JPanel(new GridLayout(1, 2, 8, 0));
        actionButtons.setBackground(panelColor);

        JButton clearCacheButton = createButton("Clear All Caches");
        JButton resetStatsButton = createButton("Reset Statistics");

        clearCacheButton.addActionListener(e -> {
            manager.clearAllCaches();
            refreshAll();
        });

        resetStatsButton.addActionListener(e -> {
            manager.resetStatistics();
            refreshAll();
        });

        actionButtons.add(clearCacheButton);
        actionButtons.add(resetStatsButton);

        content.add(actionButtons);

        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createEventLogPanel() {
        JPanel panel = createSectionPanel("EVENT LOG");
        panel.setBorder(new EmptyBorder(0, 16, 12, 16));
        panel.setPreferredSize(new Dimension(1000, 185));

        eventLogArea = new JTextArea();
        eventLogArea.setEditable(false);
        eventLogArea.setBackground(new Color(10, 15, 25));
        eventLogArea.setForeground(new Color(134, 239, 172));
        eventLogArea.setCaretColor(textColor);
        eventLogArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        eventLogArea.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(eventLogArea);
        styleScrollPane(scrollPane);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(8, 10));
        panel.setBackground(panelColor);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(55, 65, 81)),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel label = new JLabel(title);
        label.setForeground(textColor);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));

        panel.add(label, BorderLayout.NORTH);

        return panel;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(mutedColor);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(3, 0, 4, 0));
        return label;
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(new Color(55, 65, 81));
        button.setForeground(textColor);
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 8, 8, 8));
        return button;
    }

    private void styleTable(JTable table) {
        table.setBackground(new Color(24, 32, 46));
        table.setForeground(textColor);
        table.setGridColor(new Color(55, 65, 81));
        table.setSelectionBackground(new Color(30, 64, 100));
        table.setSelectionForeground(Color.WHITE);
        table.setRowHeight(25);
        table.setFont(new Font("SansSerif", Font.PLAIN, 11));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(new Color(45, 55, 72));
        table.getTableHeader().setForeground(textColor);
        table.getTableHeader().setFont(
            new Font("SansSerif", Font.BOLD, 11)
        );
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.getViewport().setBackground(new Color(24, 32, 46));
        scrollPane.setBorder(
            BorderFactory.createLineBorder(new Color(55, 65, 81))
        );
    }

    // Device management

    private void addDevice() {
        JTextField nameField = new JTextField();
        JTextField ipField = new JTextField();
        JTextField macField = new JTextField();

        JPanel form = createDeviceForm(
            nameField, ipField, macField,
            "", "", ""
        );

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            "Add Network Device",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            boolean added = manager.addDevice(
                nameField.getText().trim(),
                ipField.getText().trim(),
                macField.getText().trim()
            );

            if (!added) {
                showError("Device could not be added. Check the event log.");
            }

            refreshAll();
        }
    }

    private void editDevice() {
        int index = deviceTable.getSelectedRow();

        if (index < 0) {
            showError("Select a device to edit.");
            return;
        }

        NetworkDevice device = manager.getDevices().get(index);

        JTextField nameField = new JTextField();
        JTextField ipField = new JTextField();
        JTextField macField = new JTextField();

        JPanel form = createDeviceForm(
            nameField, ipField, macField,
            device.getDeviceName(),
            device.getIpAddress(),
            device.getMacAddress()
        );

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            "Edit Network Device",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            boolean updated = manager.updateDevice(
                index,
                nameField.getText().trim(),
                ipField.getText().trim(),
                macField.getText().trim()
            );

            if (!updated) {
                showError("Device could not be updated. Check the event log.");
            }

            refreshAll();
        }
    }

    private JPanel createDeviceForm(
            JTextField nameField,
            JTextField ipField,
            JTextField macField,
            String name,
            String ip,
            String mac) {

        nameField.setText(name);
        ipField.setText(ip);
        macField.setText(mac);

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 10));

        form.add(new JLabel("Device Name:"));
        form.add(nameField);

        form.add(new JLabel("IPv4 Address:"));
        form.add(ipField);

        form.add(new JLabel("MAC Address:"));
        form.add(macField);

        return form;
    }

    private void removeDevice() {
        int index = deviceTable.getSelectedRow();

        if (index < 0) {
            showError("Select a device to remove.");
            return;
        }

        NetworkDevice device = manager.getDevices().get(index);

        int confirmation = JOptionPane.showConfirmDialog(
            this,
            "Remove " + device.getDeviceName() + "?",
            "Confirm Removal",
            JOptionPane.YES_NO_OPTION
        );

        if (confirmation == JOptionPane.YES_OPTION) {
            boolean removed = manager.removeDevice(index);

            if (!removed) {
                showError("Device could not be removed. Check the event log.");
            }

            refreshAll();
        }
    }

    // ARP simulation

    private void resolveARP() {
        NetworkDevice sender =
            (NetworkDevice) senderCombo.getSelectedItem();

        String targetIP = (String) targetCombo.getSelectedItem();

        if (sender == null || targetIP == null) {
            showError("Select a sender and target IP.");
            return;
        }

        String result = manager.resolve(
            sender.getIpAddress(),
            targetIP
        );

        if (result != null) {
            statusLabel.setText("● Resolution Successful");
            statusLabel.setForeground(new Color(74, 222, 128));

            JOptionPane.showMessageDialog(
                this,
                "Target IP: " + targetIP
                    + "\nResolved MAC: " + result,
                "ARP Resolution Successful",
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            statusLabel.setText("● Resolution Failed");
            statusLabel.setForeground(new Color(248, 113, 113));

            JOptionPane.showMessageDialog(
                this,
                "Unable to resolve " + targetIP
                    + ".\nCheck the event log for details.",
                "ARP Resolution Failed",
                JOptionPane.WARNING_MESSAGE
            );
        }

        refreshAll();
    }

    private void applyTTL() {
        int ttl = (Integer) ttlSpinner.getValue();

        manager.setTTL(ttl);
        manager.clearAllCaches();

        refreshAll();

        JOptionPane.showMessageDialog(
            this,
            "TTL set to " + ttl
                + " seconds.\nAll caches were cleared so future "
                + "lookups use the new TTL.",
            "TTL Updated",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    // Refresh all GUI components

    private void refreshAll() {
        String oldSenderIP = getSelectedSenderIP();
        String oldTargetIP = (String) (
            targetCombo == null ? null : targetCombo.getSelectedItem()
        );
        String oldCacheIP = getSelectedCacheIP();

        refreshDeviceTable();
        refreshCombos(oldSenderIP, oldTargetIP, oldCacheIP);
        refreshCacheTable();
        refreshStatistics();
        refreshEventLog();
    }

    private void refreshDeviceTable() {
        deviceModel.setRowCount(0);

        for (NetworkDevice device : manager.getDevices()) {
            deviceModel.addRow(new Object[]{
                device.getDeviceName(),
                device.getIpAddress(),
                device.getMacAddress()
            });
        }
    }

    private void refreshCombos(
            String senderIP,
            String targetIP,
            String cacheIP) {

        List<NetworkDevice> devices = manager.getDevices();

        senderCombo.removeAllItems();
        targetCombo.removeAllItems();
        cacheDeviceCombo.removeAllItems();

        for (NetworkDevice device : devices) {
            senderCombo.addItem(device);
            cacheDeviceCombo.addItem(device);
            targetCombo.addItem(device.getIpAddress());
        }

        selectDeviceByIP(senderCombo, senderIP);
        selectDeviceByIP(cacheDeviceCombo, cacheIP);

        if (targetIP != null) {
            targetCombo.setSelectedItem(targetIP);
        }
    }

    private void selectDeviceByIP(
            JComboBox<NetworkDevice> combo,
            String ip) {

        if (ip == null) {
            return;
        }

        for (int i = 0; i < combo.getItemCount(); i++) {
            NetworkDevice device = combo.getItemAt(i);

            if (device.getIpAddress().equals(ip)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private String getSelectedSenderIP() {
        if (senderCombo == null) {
            return null;
        }

        NetworkDevice device =
            (NetworkDevice) senderCombo.getSelectedItem();

        return device == null ? null : device.getIpAddress();
    }

    private String getSelectedCacheIP() {
        if (cacheDeviceCombo == null) {
            return null;
        }

        NetworkDevice device =
            (NetworkDevice) cacheDeviceCombo.getSelectedItem();

        return device == null ? null : device.getIpAddress();
    }

    private void refreshCacheTable() {
        if (cacheModel == null || cacheDeviceCombo == null) {
            return;
        }

        cacheModel.setRowCount(0);

        NetworkDevice device =
            (NetworkDevice) cacheDeviceCombo.getSelectedItem();

        if (device == null) {
            return;
        }

        List<NetworkDevice> devices = manager.getDevices();
        int index = -1;

        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i) == device) {
                index = i;
                break;
            }
        }

        if (index < 0) {
            return;
        }

        ARPCache cache = manager.getCacheForDevice(index);
        cache.removeExpiredEntries(manager.getTTL());

        Map<String, ARPEntry> entries = cache.getEntries();
        long now = System.currentTimeMillis();

        for (ARPEntry entry : entries.values()) {
            long ageSeconds =
                (now - entry.getCreatedAt()) / 1000;

            cacheModel.addRow(new Object[]{
                entry.getIpAddress(),
                entry.getMacAddress(),
                ageSeconds
            });
        }
    }

    private void refreshStatistics() {
        NetworkStatistics stats = manager.getStatistics();

        requestLabel.setText(
            String.valueOf(stats.getTotalRequests())
        );

        replyLabel.setText(
            String.valueOf(stats.getTotalReplies())
        );

        hitLabel.setText(
            String.valueOf(stats.getCacheHits())
        );

        missLabel.setText(
            String.valueOf(stats.getCacheMisses())
        );

        failureLabel.setText(
            String.valueOf(stats.getFailedResolutions())
        );
    }

    private void refreshEventLog() {
        if (eventLogArea == null) {
            return;
        }

        List<String> events = manager.getEventLog();
        StringBuilder text = new StringBuilder();

        for (String event : events) {
            text.append(event).append("\n");
        }

        eventLogArea.setText(text.toString());
        eventLogArea.setCaretPosition(
            eventLogArea.getDocument().getLength()
        );
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "ARP Simulator",
            JOptionPane.WARNING_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception e) {
                // Use the default Swing look and feel.
            }

            ARPSimulator simulator = new ARPSimulator();
            simulator.setVisible(true);
        });
    }
}
