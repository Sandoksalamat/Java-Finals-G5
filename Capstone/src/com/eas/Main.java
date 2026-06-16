package com.eas;
import com.eas.auth.LoginFrame;
 import com.eas.util.UITheme;
 import javax.swing.SwingUtilities;
public class Main { 
    public static void main(String[] args){ 
        SwingUtilities.invokeLater(() -> { 
            UITheme.apply(); 
            new LoginFrame().setVisible(true); 
        }); 
    } 
}
