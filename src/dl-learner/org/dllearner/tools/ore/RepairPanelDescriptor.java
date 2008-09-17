/**
 * Copyright (C) 2007-2008, Jens Lehmann
 *
 * This file is part of DL-Learner.
 * 
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.dllearner.tools.ore;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.dllearner.core.owl.Individual;
import org.semanticweb.owl.model.OWLOntologyChange;




public class RepairPanelDescriptor extends WizardPanelDescriptor implements ActionListener, ListSelectionListener, MouseListener{
    
    public static final String IDENTIFIER = "REPAIR_PANEL";
    public static final String INFORMATION = "In this panel all positive and negative examples, that cause failures are shown in the list above. " +
	 "Select one of them and choose action to solve problem by press one of buttons aside the list.";
    
    private RepairPanel repairPanel;
    private Set<OWLOntologyChange> ontologyChanges;
    private ORE ore;
    private OntologyModifier modi;
   
    
    public RepairPanelDescriptor() {
        
        repairPanel = new RepairPanel();
       
        repairPanel.addActionListeners(this);
        repairPanel.addSelectionListeners(this);
        repairPanel.addMouseListeners(this);
       
        setPanelDescriptorIdentifier(IDENTIFIER);
        setPanelComponent(repairPanel);
        ontologyChanges = new HashSet<OWLOntologyChange>();
        
     
    }
    
    @Override
	public Object getNextPanelDescriptor() {
        return SavePanelDescriptor.IDENTIFIER;
    }
    
    @Override
	public Object getBackPanelDescriptor() {
        return LearningPanelDescriptor.IDENTIFIER;
    }
    
    @Override
	public void aboutToDisplayPanel() {
    	getWizard().getInformationField().setText(INFORMATION);
    }
    
    public void clearLists(){
    	repairPanel.getNegFailureModel().clear();
    	repairPanel.getPosFailureModel().clear();
    }
   
    
   

	public void valueChanged(ListSelectionEvent e) {
		
//		if (!e.getValueIsAdjusting()) 
//			 System.err.println(panel4.getNegFailureList().getSelectedValue());
		
	}

	public void actionPerformed(ActionEvent event) {
		ore = getWizardModel().getOre();
        modi = ore.getModifier();       
		String actionName = ((JButton)event.getSource()).getName();
		String actionType = ((JButton)event.getSource()).getParent().getName();
		
		if(actionType.equals("negative")){
			Individual ind = (Individual)repairPanel.getNegFailureList().getSelectedValue();
				if(actionName.equals("negRepair")){
					RepairDialog negDialog = new RepairDialog(ind, getWizard().getDialog(), ore, "neg");
					int returncode = negDialog.showDialog();
					if(returncode == 2){
						ontologyChanges.addAll(negDialog.getAllChanges());
					}
					else if(returncode == 3){
						ontologyChanges.addAll(negDialog.getAllChanges());
						repairPanel.getNegFailureModel().removeElement(ind);
					}
				}
				else if(actionName.equals("negAdd")){
					ontologyChanges.addAll(modi.addClassAssertion(ind, ore.getIgnoredConcept()));
					repairPanel.getNegFailureModel().removeElement(ind);
					
				}
				else if(actionName.equals("negDelete")){
					ontologyChanges.addAll(modi.deleteIndividual(ind));
					repairPanel.getNegFailureModel().removeElement(ind);
				
				}
		}
		else if(actionType.equals("positive")){
			Individual ind = (Individual)repairPanel.getPosFailureList().getSelectedValue();
			if(actionName.equals("posRepair")){
				RepairDialog posDialog = new RepairDialog(ind, getWizard().getDialog(), ore, "pos");
				int returncode = posDialog.showDialog();
				if(returncode == 2){
					ontologyChanges.addAll(posDialog.getAllChanges());
				}
				else if(returncode == 3){
					ontologyChanges.addAll(posDialog.getAllChanges());
					repairPanel.getPosFailureModel().removeElement(ind);
				}
			}
			else if(actionName.equals("posRemove")){
				ontologyChanges.addAll(modi.addClassAssertion(ind, ore.getIgnoredConcept()));
				repairPanel.getPosFailureModel().removeElement(ind);
				
			}
			else if(actionName.equals("posDelete")){
				ontologyChanges.addAll(modi.deleteIndividual(ind));
				repairPanel.getPosFailureModel().removeElement(ind);
				
			}
		}
		
		
	}
	
	public void mouseClicked(MouseEvent e) {
		
		if(e.getClickCount() == 2){
			if(e.getSource() == repairPanel.getNegFailureList() ){
				Individual ind = (Individual)repairPanel.getNegFailureList().getSelectedValue();
				RepairDialog negDialog = new RepairDialog(ind, getWizard().getDialog(), getWizardModel().getOre(), "neg");
				int returncode = negDialog.showDialog();
				if(returncode == 2){
					ontologyChanges.addAll(negDialog.getAllChanges());
					
				}
				else if(returncode == 3){
					ontologyChanges.addAll(negDialog.getAllChanges());
					repairPanel.getNegFailureModel().removeElement(ind);
				}
			}
			else if(e.getSource() == repairPanel.getPosFailureList()){
				Individual ind = (Individual)repairPanel.getPosFailureList().getSelectedValue();
				RepairDialog posDialog = new RepairDialog(ind, getWizard().getDialog(), getWizardModel().getOre(), "pos");
				int returncode = posDialog.showDialog();
				if(returncode == 2){
					ontologyChanges.addAll(posDialog.getAllChanges());
					
				}
				else if(returncode == 3){
					ontologyChanges.addAll(posDialog.getAllChanges());
					repairPanel.getPosFailureModel().removeElement(ind);
				}
			}
		}
		
	}

	public void mouseEntered(MouseEvent e) {
//		JList negList = repairPanel.getNegFailureList();
//		DefaultListModel negModel = repairPanel.getNegFailureModel();
//		if(e.getSource() instanceof JList){
//				int index = negList.locationToIndex(e.getPoint());
//		        if (-1 < index) {
//		        	Individual ind = (Individual)negModel.getElementAt(index);
//		        	StringBuffer strBuf = new StringBuffer();
//					strBuf.append("<html><b><u>classes:</b></u><br><br><BLOCKQUOTE>");
//														
//					for(NamedClass n: getWizardModel().getOre().reasoner2.getConcepts(ind))
//						strBuf.append("<br>" + n );
//					strBuf.append("</BLOCKQUOTE></html>");
//					negList.setToolTipText(strBuf.toString());
//		        }
//		}
		
				
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	
	public Set<OWLOntologyChange> getOntologyChanges() {
		return ontologyChanges;
	}
	
	
	
}


