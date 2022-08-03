package beastclassic.app.beauti;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.GuessPatternDialog;
import beastfx.app.inputeditor.ListInputEditor;
import beastfx.app.inputeditor.SmallLabel;
import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.datatype.UserDataType;
import beastclassic.evolution.alignment.AlignmentFromTrait;
import beastclassic.evolution.likelihood.AncestralStateTreeLikelihood;
import beast.base.evolution.tree.TraitSet;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeInterface;



public class TraitInputEditor extends ListInputEditor {

	public TraitInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> baseType() {
		return AncestralStateTreeLikelihood.class;
	}

	AncestralStateTreeLikelihood likelihood;
	TreeInterface tree;
    TraitSet traitSet;
    JTextField traitEntry;
    JComboBox relativeToComboBox;
    List<String> sTaxa;
    Object[][] tableData;
    JTable table;
    UserDataType dataType;

    //String m_sPattern = ".*(\\d\\d\\d\\d).*";
    String m_sPattern = ".*_(..).*";

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr,	ExpandOption bExpandOption, boolean bAddButtons) {
        m_bAddButtons = bAddButtons;
        m_input = input;
        m_beastObject = plugin;
        this.itemNr = itemNr;
        m_bAddButtons = bAddButtons;
		this.itemNr = itemNr;
		if (itemNr >= 0) {
			likelihood = (AncestralStateTreeLikelihood) ((ArrayList<?>)input.get()).get(itemNr);
		} else {
			likelihood = (AncestralStateTreeLikelihood) ((ArrayList<?>)input.get()).get(0);
		}
	}
//        if (((CompoundDistribution) plugin.outputs.toArray()[0]).ignoreInput.get()) {
//        	add(new Label("not in use"));
//        } else {
//        	add(new Label("Got one!"));
//        }
//	}
//
//
//
////    @Override
//    public void init2(Input<?> input, Plugin plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
	public void initPanel(AncestralStateTreeLikelihood likelihood_) {
		likelihood = likelihood_;
		m_beastObject = likelihood.dataInput.get();
		try {
			m_input = m_beastObject.getInput("traitSet");
		}catch (Exception e) {
			// TODO: handle exception
		}
		
        tree = likelihood.treeInput.get();
        if (tree != null) {
        	Alignment data = likelihood.dataInput.get();
        	if (!(data instanceof AlignmentFromTrait)) {
        		return;
        	}
    		AlignmentFromTrait traitData = (AlignmentFromTrait) data;
            m_input = traitData.traitInput;
            m_beastObject = traitData;
            traitSet = traitData.traitInput.get();
            
            if (traitSet == null) {
                traitSet = new TraitSet();
                String context = BeautiDoc.parsePartition(likelihood.getID());
                traitSet.setID("traitSet." + context);
                try {
                traitSet.initByName("traitname", "discrete",
                        "taxa", tree.getTaxonset(),
                        "value", "");
                m_input.setValue(traitSet, m_beastObject);
                data.initAndValidate();
                } catch (Exception e) {
					// TODO: handle exception
				}
            }
            
            
            dataType = (UserDataType)traitData.userDataTypeInput.get();

            Box box = Box.createVerticalBox();

            JCheckBox useTipDates = new JCheckBox("Use traits", traitSet != null);
            useTipDates.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    JCheckBox checkBox = (JCheckBox) e.getSource();
                    try {
                        Container comp = checkBox.getParent();
                        comp.removeAll();
                        if (checkBox.isSelected()) {
                            if (traitSet == null) {
                                traitSet = new TraitSet();
                                String context = BeautiDoc.parsePartition(likelihood.getID());
                                traitSet.setID("traitSet." + context);
                                traitSet.initByName("traitname", "discrete",
                                        "taxa", tree.getTaxonset(),
                                        "value", "");
                            }
                            comp.add(checkBox);
                            comp.add(createButtonBox());
                            comp.add(createListBox());
                            validateInput();
                            m_input.setValue(traitSet, m_beastObject);
                        } else {
                            m_input.setValue(null, m_beastObject);
                            comp.add(checkBox);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
            });
            //box.add(useTipDates);

            if (traitSet != null) {
                box.add(createButtonBox());
                box.add(createListBox());
            }
            add(box);
            validateInput();
            // synchronise with table, useful when taxa have been deleted
            convertTableDataToDataType();
            convertTableDataToTrait();
        }
    } // init



    private Component createListBox() {
    	try {
    		traitSet.taxaInput.get().initAndValidate();
    		
        	TaxonSet taxa = tree.getTaxonset();
        	taxa.initAndValidate();
        	sTaxa = taxa.asStringList();
    	} catch (Exception e) {
			// TODO: handle exception
            sTaxa = traitSet.taxaInput.get().asStringList();
		}
        String[] columnData = new String[]{"Name", "Trait"};
        tableData = new Object[sTaxa.size()][2];
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
            int m_iRow
                    ,
                    m_iCol;

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
                return table.getSelectedColumn() == 1;
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
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setRowHeight(24);
        JScrollPane scrollPane = new JScrollPane(table);
        return scrollPane;
    } // createListBox

    /* synchronise table with data from traitSet Plugin */
    private void convertTraitToTableData() {
        for (int i = 0; i < tableData.length; i++) {
            tableData[i][0] = sTaxa.get(i);
            tableData[i][1] = "";
        }
        String trait = traitSet.traitsInput.get();
        if (trait.trim().length() == 0) {
        	return;
        }
        String[] sTraits = trait.split(",");
        for (String sTrait : sTraits) {
            sTrait = sTrait.replaceAll("\\s+", " ");
            String[] sStrs = sTrait.split("=");
            String value = null;
            if (sStrs.length != 2) {
            	value = "";
                //throw new Exception("could not parse trait: " + sTrait);
            } else {
            	value = sStrs[1].trim();
            }
            String sTaxonID = sStrs[0].trim();
            int iTaxon = sTaxa.indexOf(sTaxonID);
            if (iTaxon < 0) {
            	System.err.println(sTaxonID);
//                throw new Exception("Trait (" + sTaxonID + ") is not a known taxon. Spelling error perhaps?");
            } else {
	            tableData[iTaxon][0] = sTaxonID;
	            tableData[iTaxon][1] = value;
            }
        }

        if (table != null) {
            for (int i = 0; i < tableData.length; i++) {
                table.setValueAt(tableData[i][1], i, 1);
            }
        }
    } // convertTraitToTableData

    /**
     * synchronise traitSet Plugin with table data
     */
    private void convertTableDataToTrait() {
        String sTrait = "";
        //Set<String> values = new HashSet<String>(); 
        for (int i = 0; i < tableData.length; i++) {
            sTrait += sTaxa.get(i) + "=" + tableData[i][1];
            if (i < tableData.length - 1) {
                sTrait += ",\n";
            }
        }
        try {
            traitSet.traitsInput.setValue(sTrait, traitSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        convertTableDataToDataType();
    }

    private void convertTableDataToDataType() {
        List<String> values = new ArrayList<String>(); 
        for (int i = 0; i < tableData.length; i++) {
        	if (tableData[i][1].toString().trim().length() > 0 && !values.contains(tableData[i][1].toString())) {
        		values.add(tableData[i][1].toString());
        	}
        }
        Collections.sort(values);
        String codeMap = "";
        int k = 0;
        for (String value : values) {
        	codeMap += value + "=" + k + ",";
        	k++;
        }
        // add unknown/missing character
        codeMap += "? = ";
        for (int i = 0; i < values.size(); i++) {
        	codeMap += i + " ";
        }
        // System.err.println(codeMap);
        try {
            dataType.codeMapInput.setValue(codeMap, dataType);
            dataType.stateCountInput.setValue(values.size(), dataType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        validateInput();
    }

    /**
     * create box with comboboxes for selection units and trait name *
     */
    private Box createButtonBox() {
        Box buttonBox = Box.createHorizontalBox();

        JLabel label = new JLabel("Trait: ");
        //label.setMaximumSize(new Dimension(1024, 20));
        buttonBox.add(label);

        traitEntry = new JTextField(traitSet.traitNameInput.get());
        traitEntry.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {update();}
			@Override
			public void insertUpdate(DocumentEvent e) {update();}
			@Override
			public void changedUpdate(DocumentEvent e) {update();}
			void update() {
				try {
					traitSet.traitNameInput.setValue(traitEntry.getText(), traitSet);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		});
        traitEntry.setColumns(12);
        buttonBox.add(traitEntry);
        buttonBox.add(Box.createHorizontalGlue());

        JButton guessButton = new JButton("Guess");
        guessButton.setName("guess");
        guessButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	guess();
            }
        });
        buttonBox.add(guessButton);


        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    traitSet.traitsInput.setValue("", traitSet);
                    convertTableDataToDataType();
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
    
    
    private void guess() {
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
        try {
        	traitSet.traitsInput.setValue(sTrait, traitSet);
        } catch (Exception e) {
			// TODO: handle exception
		}
        convertTraitToTableData();
        convertTableDataToTrait();
        convertTableDataToDataType();
        repaint();
    }
	
	@Override
	public void validateInput() {
		// check all values are specified
		if (tableData == null) {
			return;
		}
        for (int i = 0; i < tableData.length; i++) {
        	if (tableData[i][1].toString().trim().length() == 0) {
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
