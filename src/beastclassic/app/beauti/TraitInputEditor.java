package beastclassic.app.beauti;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.GuessPatternDialog;
import beastfx.app.inputeditor.ListInputEditor;
import beastfx.app.inputeditor.SmallLabel;
import beastfx.app.util.FXUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.datatype.UserDataType;
import beastclassic.evolution.alignment.AlignmentFromTrait;
import beastclassic.evolution.likelihood.AncestralStateTreeLikelihood;
import beast.base.evolution.tree.TraitSet;
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
    TextField traitEntry;
    //ComboBox relativeToComboBox;
    List<String> sTaxa;
    // Object[][] tableData;
    
    public class LocationMap {
		String taxon;
    	String trait;

    	LocationMap(String taxon, String trait) {
    		this.taxon = taxon;
    		this.trait = trait;
    	}
    	
    	public String getTaxon() {
			return taxon;
		}
		public void setTaxon(String taxon) {
			this.taxon = taxon;
		}
		public String getTrait() {
			return trait;
		}
		public void setTrait(String trait) {
			this.trait = trait;
		}
    }
    
    TableView<LocationMap> table;
    ObservableList<LocationMap> taxonMapping;    
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

            VBox box = FXUtils.newVBox();

            CheckBox useTipDates = new CheckBox("Use traits");
            useTipDates.setSelected(traitSet != null);
            useTipDates.setOnAction(e -> {
                    try {
                        Pane comp = (Pane) useTipDates.getParent();
                        comp.getChildren().removeAll();
                        if (useTipDates.isSelected()) {
                            if (traitSet == null) {
                                traitSet = new TraitSet();
                                String context = BeautiDoc.parsePartition(likelihood.getID());
                                traitSet.setID("traitSet." + context);
                                traitSet.initByName("traitname", "discrete",
                                        "taxa", tree.getTaxonset(),
                                        "value", "");
                            }
                            comp.getChildren().add(useTipDates);
                            comp.getChildren().add(createButtonBox());
                            comp.getChildren().add(createListBox());
                            validateInput();
                            m_input.setValue(traitSet, m_beastObject);
                        } else {
                            m_input.setValue(null, m_beastObject);
                            comp.getChildren().add(useTipDates);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
            });
            //box.add(useTipDates);

            if (traitSet != null) {
                box.getChildren().add(createButtonBox());
                box.getChildren().add(createListBox());
            }
            getChildren().add(box);
            validateInput();
            // synchronise with table, useful when taxa have been deleted
            convertTableDataToDataType();
            convertTableDataToTrait();
        }
    } // init



    private TableView createListBox() {
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
        taxonMapping = FXCollections.observableArrayList();
        for (String s : sTaxa) {
        	taxonMapping.add(new LocationMap(s, ""));
        }
        // tableData = new Object[sTaxa.size()][2];
        convertTraitToTableData();
        
        
        
        // set up table.
        // special features: background shading of rows
        // custom editor allowing only Date column to be edited.
//      for (Taxon taxonset2 : m_taxonset) {
//      	if (taxonset2 instanceof TaxonSet) {
//		        for (Taxon taxon : ((TaxonSet) taxonset2).taxonsetInput.get()) {
//		            m_lineageset.add(taxon);
//		            m_taxonMap.put(taxon.getID(), taxonset2.getID());
//		            taxonMapping.add(new TaxonMap(taxon.getID(), taxonset2.getID()));
//		        }
//      	}
//      }

      // set up table.
      // special features: background shading of rows
      // custom editor allowing only Date column to be edited.
      table = new TableView<>();        
      table.setPrefWidth(1024);
      table.setEditable(true);
      table.setItems(taxonMapping);

      TableColumn<LocationMap, String> col1 = new TableColumn<>("Taxon");
      col1.setPrefWidth(500);
      col1.setEditable(false);
      col1.setCellValueFactory(
      	    new PropertyValueFactory<LocationMap,String>("Taxon")
      	);
      table.getColumns().add(col1);        

      TableColumn<LocationMap, String> col2 = new TableColumn<>("Trait");
      col2.setPrefWidth(500);
      col2.setEditable(true);
      col2.setCellValueFactory(
      	    new PropertyValueFactory<LocationMap,String>("Trait")
      	);
      col2.setCellFactory(TextFieldTableCell.forTableColumn());
      col2.setOnEditCommit(
              new EventHandler<CellEditEvent<LocationMap, String>>() {
					@Override
					public void handle(CellEditEvent<LocationMap, String> event) {
						String newValue = event.getNewValue();
						LocationMap location = event.getRowValue();
						location.setTrait(newValue);
						convertTableDataToTrait();
  						validateInput();
					}
				}                
          );
      
      table.getColumns().add(col2);        
      
//        table = new JTable(tableData, columnData) {
//            private static final long serialVersionUID = 1L;
//
//            // method that induces table row shading
//            @Override
//            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
//                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
//                //even index, selected or not selected
//                if (isCellSelected(Index_row, Index_col)) {
//                    comp.setBackground(Color.lightGray);
//                } else if (Index_row % 2 == 0 && !isCellSelected(Index_row, Index_col)) {
//                    comp.setBackground(new Color(237, 243, 255));
//                } else {
//                    comp.setBackground(Color.white);
//                }
//                return comp;
//            }
//        };
//
//        // set up editor that makes sure only doubles are accepted as entry
//        // and only the Date column is editable.
//        table.setDefaultEditor(Object.class, new TableCellEditor() {
//            JTextField m_textField = new JTextField();
//            int m_iRow
//                    ,
//                    m_iCol;
//
//            @Override
//            public boolean stopCellEditing() {
//                table.removeEditor();
//                String sText = m_textField.getText();
//                if (sText == "") {
//                	return false;
//                }
//                tableData[m_iRow][m_iCol] = sText;
//                convertTableDataToTrait();
//                convertTraitToTableData();
//                validateInput();
//                return true;
//            }
//
//            @Override
//            public boolean isCellEditable(EventObject anEvent) {
//                return table.getSelectedColumn() == 1;
//            }
//
//
//            @Override
//            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int iRow, int iCol) {
//                if (!isSelected) {
//                    return null;
//                }
//                m_iRow = iRow;
//                m_iCol = iCol;
//                m_textField.setText((String) value);
//                return m_textField;
//            }
//
//            @Override
//            public boolean shouldSelectCell(EventObject anEvent) {
//                return false;
//            }
//
//            @Override
//            public void removeCellEditorListener(CellEditorListener l) {
//            }
//
//            @Override
//            public Object getCellEditorValue() {
//                return null;
//            }
//
//            @Override
//            public void cancelCellEditing() {
//            }
//
//            @Override
//            public void addCellEditorListener(CellEditorListener l) {
//            }
//
//        });
//        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
//        table.setRowHeight(24);
        return table;
    } // createListBox

    /* synchronise table with data from traitSet Plugin */
    private void convertTraitToTableData() {
        for (int i = 0; i < taxonMapping.size(); i++) {
        	taxonMapping.get(i).taxon = sTaxa.get(i);
        	taxonMapping.get(i).trait = "";
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
            	taxonMapping.get(iTaxon).taxon = sTaxonID;
            	taxonMapping.get(iTaxon).trait = value;
            }
        }

        if (table != null) {
            table.refresh();
//            for (int i = 0; i < tableData.length; i++) {
//                table.setValueAt(tableData[i][1], i, 1);
//            }
        }
    } // convertTraitToTableData

    /**
     * synchronise traitSet Plugin with table data
     */
    private void convertTableDataToTrait() {
        String sTrait = "";
        //Set<String> values = new HashSet<String>(); 
        for (int i = 0; i < taxonMapping.size(); i++) {
            sTrait += taxonMapping.get(i).taxon + "=" + taxonMapping.get(i).trait;
            if (i < taxonMapping.size() - 1) {
                sTrait += ",\n";
            }
        }
        try {
System.err.println("TRAIT=" + sTrait);

            traitSet.traitsInput.setValue(sTrait, traitSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        convertTableDataToDataType();
    }

    private void convertTableDataToDataType() {
        List<String> values = new ArrayList<String>(); 
        for (int i = 0; i < taxonMapping.size(); i++) {
        	if (taxonMapping.get(i).trait.trim().length() > 0 && !values.contains(taxonMapping.get(i).trait)) {
        		values.add(taxonMapping.get(i).trait);
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
    private HBox createButtonBox() {
        HBox buttonBox = FXUtils.newHBox();

        Label label = new Label("Trait: ");
        //label.setMaximumSize(new Dimension(1024, 20));
        buttonBox.getChildren().add(label);

        traitEntry = new TextField(traitSet.traitNameInput.get());
        traitEntry.setOnKeyReleased(e->{
			try {
				traitSet.traitNameInput.setValue(traitEntry.getText(), traitSet);
			} catch (Exception ex) {
				// TODO: handle exception
			}        	
        });
//        traitEntry.getDocument().addDocumentListener(new DocumentListener() {
//			
//			@Override
//			public void removeUpdate(DocumentEvent e) {update();}
//			@Override
//			public void insertUpdate(DocumentEvent e) {update();}
//			@Override
//			public void changedUpdate(DocumentEvent e) {update();}
//			void update() {
//			}
//		});
        //traitEntry.setColumns(12);
        traitEntry.setMinWidth(12*15);
        buttonBox.getChildren().add(traitEntry);
        //buttonBox.add(Box.createHorizontalGlue());

        Button guessButton = new Button("Guess");
        guessButton.setId("guess");
        guessButton.setOnAction(e->guess());
        buttonBox.getChildren().add(guessButton);


        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e-> {
                try {
                    traitSet.traitsInput.setValue("", traitSet);
                    convertTraitToTableData();
                    convertTableDataToDataType();
                    if (table != null) {
                        table.refresh();
                    }
                } catch (Exception ex) {
                    // TODO: handle exception
                }
                refreshPanel();
            });
        buttonBox.getChildren().add(clearButton);

        m_validateLabel = new SmallLabel("x", "orange");
        m_validateLabel.setVisible(false);
        buttonBox.getChildren().add(m_validateLabel);
        
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
		if (taxonMapping == null) {
			return;
		}
        for (int i = 0; i < taxonMapping.size(); i++) {
        	if (taxonMapping.get(i).trait.trim().length() == 0) {
        		m_validateLabel.setVisible(true);
        		m_validateLabel.setTooltip("trait for " + taxonMapping.get(i).taxon + " needs to be specified");
        		// m_validateLabel.repaint();
        		return;
        	}
        }
		m_validateLabel.setVisible(false);
		super.validateInput();
	}
}
