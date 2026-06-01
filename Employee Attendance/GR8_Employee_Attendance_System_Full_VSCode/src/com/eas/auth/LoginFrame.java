package com.eas.auth;
import com.eas.config.Database;
 import com.eas.model.UserSession;
 import com.eas.service.AuthService;
 import com.eas.ui.admin.AdminDashboardFrame;
 import com.eas.ui.user.UserDashboardFrame;
 import com.eas.util.*;
 import java.awt.*;
 import javax.swing.*;
public class LoginFrame extends JFrame { private final JTextField username=new JTextField(20); private final JPasswordField password=new JPasswordField(20);
 public LoginFrame(){setTitle("GR 5 - Employee Attendance System | Login");setDefaultCloseOperation(EXIT_ON_CLOSE);setSize(540,430);setLocationRelativeTo(null);JPanel root=new JPanel(new BorderLayout());root.setBorder(BorderFactory.createEmptyBorder(28,50,25,50));JLabel heading=UITheme.title("EMPLOYEE ATTENDANCE SYSTEM");heading.setHorizontalAlignment(SwingConstants.CENTER);root.add(heading,BorderLayout.NORTH);JPanel form=new JPanel(new GridLayout(0,1,6,6));form.setBorder(BorderFactory.createEmptyBorder(30,15,20,15));form.add(new JLabel("Username"));form.add(username);form.add(new JLabel("Password"));form.add(password);JButton login=UITheme.button("LOGIN");JButton register=UITheme.button("REGISTER EMPLOYEE ACCOUNT");JButton test=UITheme.button("TEST DATABASE CONNECTION");form.add(login);form.add(register);form.add(test);root.add(form,BorderLayout.CENTER);JLabel hint=new JLabel("Demo Admin: admin / Admin@123    Demo Employee: employee1 / User@123");hint.setHorizontalAlignment(SwingConstants.CENTER);root.add(hint,BorderLayout.SOUTH);add(root);login.addActionListener(e->login());register.addActionListener(e->{dispose();new RegisterFrame().setVisible(true);});test.addActionListener(e->testConnection());getRootPane().setDefaultButton(login);}
 private void login(){try{UserSession s=new AuthService().login(username.getText(),new String(password.getPassword()));if(s==null){Dialogs.error(this,"Invalid username or password.");return;}dispose();if(s.isAdmin())new AdminDashboardFrame(s).setVisible(true);else new UserDashboardFrame(s).setVisible(true);}catch(Exception ex){Dialogs.error(this,ex.getMessage());}}
 private void testConnection(){try{Dialogs.info(this,Database.testConnection()?"Database connection successful.":"Database connection failed.");}catch(Exception ex){Dialogs.error(this,ex.getMessage());}}
}
