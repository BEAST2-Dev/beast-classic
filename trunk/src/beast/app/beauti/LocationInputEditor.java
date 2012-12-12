package beast.app.beauti;


import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import beast.app.draw.ListInputEditor;
import beast.app.draw.SmallLabel;
import beast.continuous.SampledMultivariateTraitLikelihood;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.AlignmentFromTraitMap;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeTraitMap;

public class LocationInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;

	public LocationInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> baseType() {
		return SampledMultivariateTraitLikelihood.class;
	}

	SampledMultivariateTraitLikelihood likelihood;
	Tree tree;
    TreeTraitMap traitSet;
    //JTextField traitEntry;
    JComboBox relativeToComboBox;
    String [] sTaxa;
    Object[][] tableData;
    JTable table;
    //UserDataType dataType;

    //String m_sPattern = ".*(\\d\\d\\d\\d).*";
    String m_sPattern = ".*_(..).*";

	@Override
	public void init(Input<?> input, Plugin plugin, int itemNr,	ExpandOption bExpandOption, boolean bAddButtons) {
        m_bAddButtons = bAddButtons;
        m_input = input;
        m_plugin = plugin;
        this.itemNr = itemNr;
        m_bAddButtons = bAddButtons;
		this.itemNr = itemNr;
		if (itemNr >= 0) {
			likelihood = (SampledMultivariateTraitLikelihood) ((ArrayList<?>)input.get()).get(itemNr);
		} else {
			likelihood = (SampledMultivariateTraitLikelihood) ((ArrayList<?>)input.get()).get(0);
		}
	}

	public void initPanel(SampledMultivariateTraitLikelihood likelihood_) {
		likelihood = likelihood_;
		m_plugin = likelihood.m_data.get();
		try {
			m_input = m_plugin.getInput("traitSet");
		}catch (Exception e) {
			// TODO: handle exception
		}
		
        tree = likelihood.m_tree.get();
        if (tree != null) {
        	Alignment data = likelihood.m_data.get();
        	if (!(data instanceof AlignmentFromTraitMap)) {
        		return;
        	}
    		AlignmentFromTraitMap traitData = (AlignmentFromTraitMap) data;
            m_input = traitData.traitInput;
            m_plugin = traitData;
            traitSet = traitData.traitInput.get();
            
            Box box = Box.createVerticalBox();

            if (traitSet != null) {
                box.add(createButtonBox());
                box.add(createListBox());
                box.add(createButtonBox2());
            }
            add(box);
            validateInput();
            // synchronise with table, useful when taxa have been deleted
            convertTableDataToTrait();
        }
    } // init



    private Component createListBox() {
    	try {
    		traitSet.treeInput.get().getTaxaNames();
    	} catch (Exception e) {
			// TODO: handle exception
		}
        sTaxa = traitSet.treeInput.get().getTaxaNames();
        String[] columnData = new String[]{"Name", "Latitude", "Longitude"};
        tableData = new Object[sTaxa.length][3];
        convertTraitToTableData();
        // set up table.
        // special features: background shading of rows
        // custom editor allowing only Date column to be edited.
        table = new JTable(tableData, columnData) {
            private static final long serialVersionUID = 1L;

            // method that induces table row shading
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
                //even index, selected or not selected
                if (isCellSelected(Index_row, Index_col)) {
                    comp.setBackground(Color.lightGray);
                } else if (Index_row % 2 == 0 && !isCellSelected(Index_row, Index_col)) {
                    comp.setBackground(new Color(237, 243, 255));
                } else {
                    comp.setBackground(Color.white);
                }
                return comp;
            }
        };

        // set up editor that makes sure only doubles are accepted as entry
        // and only the Date column is editable.
        table.setDefaultEditor(Object.class, new TableCellEditor() {
            JTextField m_textField = new JTextField();
            int m_iRow, m_iCol;

            @Override
            public boolean stopCellEditing() {
                table.removeEditor();
                String sText = m_textField.getText();
                if (sText == "") {
                	return false;
                }
                tableData[m_iRow][m_iCol] = sText;
                convertTableDataToTrait();
                convertTraitToTableData();
                validateInput();
                return true;
            }

            @Override
            public boolean isCellEditable(EventObject anEvent) {
                return table.getSelectedColumn() >= 1;
            }


            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int iRow, int iCol) {
                if (!isSelected) {
                    return null;
                }
                m_iRow = iRow;
                m_iCol = iCol;
                m_textField.setText((String) value);
                return m_textField;
            }

            @Override
            public boolean shouldSelectCell(EventObject anEvent) {
                return false;
            }

            @Override
            public void removeCellEditorListener(CellEditorListener l) {
            }

            @Override
            public Object getCellEditorValue() {
                return null;
            }

            @Override
            public void cancelCellEditing() {
            }

            @Override
            public void addCellEditorListener(CellEditorListener l) {
            }

        });
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(75);
        table.getColumnModel().getColumn(2).setPreferredWidth(75);
        JScrollPane scrollPane = new JScrollPane(table);
        return scrollPane;
    } // createListBox

    /* synchronise table with data from traitSet Plugin */
    private void convertTraitToTableData() {
        for (int i = 0; i < tableData.length; i++) {
            tableData[i][0] = sTaxa[i];
            tableData[i][1] = "";
            tableData[i][2] = "";
        }
        String trait = traitSet.value.get();
        if (trait == null || trait.trim().length() == 0) {
        	return;
        }
        String[] sTraits = trait.split(",");
        for (String sTrait : sTraits) {
            //sTrait = sTrait.replaceAll("\\s+", " ");
            String[] sStrs = sTrait.split("=");
            String value = null;
            if (sStrs.length != 2) {
            	value = "";
                //throw new Exception("could not parse trait: " + sTrait);
            } else {
            	value = sStrs[1].trim();
            }
            String sTaxonID = sStrs[0].trim();
            int iTaxon = indexOf(sTaxonID);
            if (iTaxon < 0) {
            	System.err.println(sTaxonID);
//                throw new Exception("Trait (" + sTaxonID + ") is not a known taxon. Spelling error perhaps?");
            } else {
	            tableData[iTaxon][0] = sTaxonID;
	            String [] sStrs2 = value.trim().split("\\s+");
	            if (sStrs2.length == 2) {
	            	tableData[iTaxon][1] = sStrs2[0];
	            	tableData[iTaxon][2] = sStrs2[1];
	            } else {
	            	if (Character.isSpace(sStrs[1].charAt(0))) {
	            		tableData[iTaxon][1] = "";
		            	tableData[iTaxon][2] = sStrs2[0];
	            	} else {
		            	tableData[iTaxon][1] = sStrs2[0];
		            	tableData[iTaxon][2] = "";
	            	}
	            }
            }
        }

        if (table != null) {
            for (int i = 0; i < tableData.length; i++) {
                table.setValueAt(tableData[i][1], i, 1);
                table.setValueAt(tableData[i][2], i, 2);
            }
        }
    } // convertTraitToTableData

    private int indexOf(String sTaxonID) {
		for (int i = 0; i < sTaxa.length; i++) {
			if (sTaxa[i].equals(sTaxonID)) {
				return i;
			}
		}
		return -1;
	}

	/**
     * synchronise traitSet Plugin with table data
     */
    void convertTableDataToTrait() {
        String sTrait = "";
        //Set<String> values = new HashSet<String>(); 
        for (int i = 0; i < tableData.length; i++) {
            sTrait += sTaxa[i] + "=" + tableData[i][1] + " " + tableData[i][2];
            if (i < tableData.length - 1) {
                sTrait += ",\n";
            }
        }
        try {
            traitSet.value.setValue(sTrait, traitSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * create box with comboboxes for selection units and trait name *
     */
    private Box createButtonBox() {
        Box buttonBox = Box.createHorizontalBox();

        JLabel label = new JLabel("Trait: ");
        //label.setMaximumSize(new Dimension(1024, 20));
        buttonBox.add(label);

//        traitEntry = new JTextField(traitSet.m_sTraitName.get());
//        traitEntry.getDocument().addDocumentListener(new DocumentListener() {
//			
//			@Override
//			public void removeUpdate(DocumentEvent e) {update();}
//			@Override
//			public void insertUpdate(DocumentEvent e) {update();}
//			@Override
//			public void changedUpdate(DocumentEvent e) {update();}
//			void update() {
//				try {
//					traitSet.m_sTraitName.setValue(traitEntry.getText(), traitSet);
//				} catch (Exception e) {
//					// TODO: handle exception
//				}
//			}
//		});
//        traitEntry.setColumns(12);
//        buttonBox.add(traitEntry);
        buttonBox.add(Box.createHorizontalGlue());

        JButton guessButton = new JButton("Guess latitude");
        guessButton.setName("Guess latitude");
        guessButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	guess(1);
            }
        });
        buttonBox.add(guessButton);
        
        JButton guessButton2 = new JButton("Guess longitude");
        guessButton2.setName("Guess longitude");        
        guessButton2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	guess(2);
            }
        });
        buttonBox.add(guessButton2);


        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    traitSet.value.setValue("", traitSet);
                } catch (Exception ex) {
                    // TODO: handle exception
                }
                refreshPanel();
            }
        });
        buttonBox.add(clearButton);

        m_validateLabel = new SmallLabel("x", Color.orange);
        m_validateLabel.setVisible(false);
        buttonBox.add(m_validateLabel);
        
        return buttonBox;
    } // createButtonBox
    
    
    private void guess(int column) {
        GuessPatternDialog dlg = new GuessPatternDialog(this, m_sPattern);
        //dlg.setName("GuessPatternDialog");
        String sTrait = "";
        switch (dlg.showDialog("Guess traits from taxon names")) {
        case canceled : return;
        case trait: sTrait = dlg.getTrait();
        	break;
        case pattern:
            String sPattern = dlg.getPattern(); 
            try {
                Pattern pattern = Pattern.compile(sPattern);
                for (String sTaxon : sTaxa) {
                    Matcher matcher = pattern.matcher(sTaxon);
                    if (matcher.find()) {
                        String sMatch = matcher.group(1);
                        if (sTrait.length() > 0) {
                            sTrait += ",";
                        }
                        sTrait += sTaxon + "=" + sMatch;
                    }
                    m_sPattern = sPattern;
                }
            } catch (Exception e) {
                return;
            }
            break;
        }
    	String [] strs = sTrait.trim().split(",");
    	for (String str : strs) {
    		String [] strs2 = str.trim().split("=");
    		String taxon = strs2[0].trim();
    		String value = strs2[1].trim();
    		for (int i = 0; i < tableData.length; i++) {
    			if (tableData[i][0].equals(taxon)) {
    				tableData[i][column] = value;
    				break;
    			}
    		}
    	}
        convertTableDataToTrait();
        validateInput();
        //convertTraitToTableData();
        repaint();
    }
	

    private Box createButtonBox2() {
        Box buttonBox = Box.createHorizontalBox();

        buttonBox.add(Box.createHorizontalGlue());

        JButton manipulateButton = new JButton("Manipulate latitude");
        manipulateButton.setName("Manipulate latitude");
        manipulateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	manipulate(1);
            }
        });
        buttonBox.add(manipulateButton);
        
        JButton manipulateButton2 = new JButton("Manipulate longitude");
        manipulateButton2.setName("Manipulate longitude");        
        manipulateButton2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	manipulate(2);
            }
        });
        buttonBox.add(manipulateButton2);
        return buttonBox;
    } // createButtonBox2
    
    
    // a JavaScript engine
    static ScriptEngine m_engine;
    {
		// create a script engine manager
	    ScriptEngineManager factory = new ScriptEngineManager();
	    // create a JavaScript engine
	    m_engine = factory.getEngineByName("JavaScript");
    }

    private void manipulate(int column) {
		String operatee = (column == 1 ? "latitude" : "longitude");
		String formula = JOptionPane.showInputDialog(this, "<html>Give a formula with $x as the " + operatee + 
				" e.g., -$x to make values negative<br>" +
				"180+$x to add 180 to " + operatee + "<br>" +
				"$x*2+10 to multiply by 2 and add 10<br>" +
				"max($x,100) to get the maximum of " + operatee + " and 100", "Manipulate " + operatee, JOptionPane.OK_CANCEL_OPTION);
		if (formula == null || formula.trim().length() == 0) {
			return;
		}
		
		for (int i = 0; i < tableData.length; i++) {
			String value = tableData[i][column].toString();
			try {
				value = Double.parseDouble(value) + "";
			} catch (Exception e) {
				value = "0";
			}
			String newValue = value(formula, value);
			tableData[i][column] = newValue;
		}
		convertTableDataToTrait();
		validateInput();
		repaint();
	}

	String value(String formula, String value) {
		String sFormula = "with (Math) {" + formula + "}";
		sFormula = sFormula.replaceAll("\\$x", "("+value+")");
		System.err.println("parsing " + sFormula);
		try {
			Object o = m_engine.eval(sFormula);
			String sValue = o.toString();
			return sValue;
		} catch (javax.script.ScriptException es) {
			return es.getMessage();
		}
	}

    
	@Override
	public void validateInput() {
		// check all values are specified
		if (tableData == null) {
			return;
		}
        for (int i = 0; i < tableData.length; i++) {
        	if (tableData[i][1].toString().trim().length() == 0 || tableData[i][2].toString().trim().length() == 0) {
        		m_validateLabel.setVisible(true);
        		m_validateLabel.setToolTipText("trait for " + tableData[i][0] + " needs to be specified");
        		m_validateLabel.repaint();
        		return;
        	}
        }
		m_validateLabel.setVisible(false);
		super.validateInput();
	}
}
