package com.bormannqds.mds.tools.monitoringtool.gui;

import com.bormannqds.mds.tools.monitoringtool.MonitoringTool;
import com.bormannqds.lib.bricks.gui.AbstractPanel;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * Created by bormanng on 10/07/15.
 */
public class MonitoringToolPanel extends AbstractPanel {
    public MonitoringToolPanel(final TableModel marketDataModel) {
        super(new BorderLayout(0, 0));
        initialise(marketDataModel);
    }

    private void initialise(final TableModel marketDataModel) {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        marketDataTable.setToolTipText("Market data");
        marketDataTable.setModel(marketDataModel);
        // TODO Figure out sensible column size/resizing properties
        marketDataTable.getTableHeader().setReorderingAllowed(false);
        marketDataTable.getTableHeader().setAlignmentX(CENTER_ALIGNMENT);
        {
            final JPanel centrePanel = new JPanel();
            centrePanel.setMinimumSize(new Dimension(130, 100));
            centrePanel.setPreferredSize(new Dimension(130, 120));
            add(centrePanel, BorderLayout.CENTER);
            final GroupLayout groupLayout = new GroupLayout(centrePanel);
            centrePanel.setLayout(groupLayout);
            {
                final JPanel tablePanel = new JPanel();
                {
                    final GroupLayout tpGroupLayout = new GroupLayout(tablePanel);
                    tablePanel.setLayout(tpGroupLayout);
                    tpGroupLayout.setHorizontalGroup(
                            tpGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(tpGroupLayout.createSequentialGroup().addContainerGap().addComponent(marketDataTable.getTableHeader(), GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGroup(tpGroupLayout.createSequentialGroup().addContainerGap().addComponent(marketDataTable, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                    );
                    tpGroupLayout.setVerticalGroup(
                            tpGroupLayout.createSequentialGroup()
                                .addGroup(tpGroupLayout.createSequentialGroup()
                                    .addContainerGap().addComponent(marketDataTable.getTableHeader(), 25, 25, 25))
                                    .addComponent(marketDataTable)
                                    .addContainerGap()
                    );
                }
                final JScrollPane scrollPane = new JScrollPane(tablePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                groupLayout.setHorizontalGroup(
                        groupLayout.createSequentialGroup()
                        .addGroup(groupLayout.createSequentialGroup().addGap(5).addComponent(scrollPane))
                );
                groupLayout.setVerticalGroup(
                        groupLayout.createSequentialGroup()
                                .addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                );
            }
        }
    }

    private final JTable marketDataTable = new JTable();
}
