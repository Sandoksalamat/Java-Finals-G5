package com.eas.util;
import java.awt.*;
 import javax.swing.*;
public final class UITheme { 
    public static final Color NAVY=new Color(18,45,72), 
    TEAL=new Color(21,126,139), 
    LIGHT=new Color(244,247,250); 
    
    private UITheme(){} 
    
    public static void apply(){ 
        try{
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
            
            catch(Exception ignored){} 
            
            UIManager.put("Panel.background",LIGHT); 
            UIManager.put("TabbedPane.background",LIGHT); 
        } 
        
        public static JButton button(String text){
            JButton b=new JButton(text);
            b.setBackground(TEAL);
            b.setForeground(Color.BLACK);
            b.setFocusPainted(false);
            b.setFont(new Font("SansSerif",Font.BOLD,12));
            return b;
        } 
        
        public static JLabel title(String t){
            JLabel l=new JLabel(t);
            l.setFont(new Font("SansSerif",Font.BOLD,22));
            l.setForeground(NAVY);return l;
        } 
    }
