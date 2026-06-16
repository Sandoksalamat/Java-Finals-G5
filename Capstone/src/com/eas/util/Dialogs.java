package com.eas.util;
import java.awt.Component; import javax.swing.JOptionPane;
public final class Dialogs { private Dialogs(){} public static void info(Component c,String m){JOptionPane.showMessageDialog(c,m,"Employee Attendance System",JOptionPane.INFORMATION_MESSAGE);} public static void error(Component c,String m){JOptionPane.showMessageDialog(c,m,"Error",JOptionPane.ERROR_MESSAGE);} public static boolean confirm(Component c,String m){return JOptionPane.showConfirmDialog(c,m,"Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;} }
