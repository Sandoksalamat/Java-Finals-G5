package com.eas.ui.user;
import com.eas.config.Database;
 import com.eas.model.UserSession;
 import com.eas.service.*;
 import com.eas.util.*;
 import java.awt.*;
 import java.io.File;
 import java.sql.*;
 import javax.swing.*;
public class UserProfilePanel extends JPanel { 
    private final UserSession session;
    private final JTextField 
    full = new JTextField(22),
    email = new JTextField(22),
    phone = new JTextField(15),
    address = new JTextField(30),
    emergency = new JTextField(25),
    allergy = new JTextField(30),
    exist_medcon = new JTextField(30);      
    
    
    private final JPasswordField 
    oldp = new JPasswordField(15),
    newp = new JPasswordField(15); 
    
    public UserProfilePanel(UserSession s){
        session=s;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20,30,20,30));
        add(UITheme.title("My Profile and Password"),BorderLayout.NORTH);

        JPanel f = new JPanel(new GridLayout(0,2,8,8));
        f.setBorder(BorderFactory.createTitledBorder("Employee Details"));
        add(f,"Full Name",full);
        add(f,"Email",email);
        add(f,"Phone",phone);
        add(f,"Address",address);
        add(f,"Emergency Contact",emergency);

        JButton save=UITheme.button("SAVE PROFILE");

        f.add(save);
        f.add(new JLabel(""));

        JPanel pwd=new JPanel();
        pwd.setBorder(BorderFactory.createTitledBorder("Change Password"));
        pwd.add(new JLabel("Current"));
        pwd.add(oldp);
        pwd.add(new JLabel("New"));
        pwd.add(newp);
        JButton change=UITheme.button("CHANGE PASSWORD");
        pwd.add(change);

        JPanel p = new JPanel(new GridLayout(0, 3, 8, 8));
        p.setBorder(BorderFactory.createTitledBorder("Employee Medical Details"));

        JLabel btype = new JLabel("Bloodtype");
        String[] btypeSelect = {"A+", "A-", "AB+", "AB-", "B+", "B-", "O+", "O-", };
        JComboBox<String> btypeDropdown = new JComboBox<>(btypeSelect);

        p.add(btype);
        p.add(btypeDropdown);


        JLabel mednoteLbl = new JLabel("Emergency Medical Notes");
        JTextArea mednote = new JTextArea();

        p.add(mednoteLbl);
        p.add(mednote);
        
        add(p,"Allergy",allergy);
        add(p,"Existing Medical Condition", exist_medcon);

        p.add(save);
        p.add(new JLabel(""));
        
        JLabel certLabel = new JLabel("No file selected");
        JButton certBtn = UITheme.button("Attach");
        certBtn.addActionListener(e -> attachFile(certLabel));

        p.add(new JLabel("Medical Certificate"));
        p.add(certLabel);
        p.add(certBtn);

        JLabel injuryLabel = new JLabel("No file selected");
        JButton injuryBtn = UITheme.button("Attach");
        injuryBtn.addActionListener(e -> attachFile(injuryLabel));

        p.add(new JLabel("Workplace Injury Report"));
        p.add(injuryLabel);
        p.add(injuryBtn);

        JLabel healthLabel = new JLabel("No file selected");
        JButton healthBtn = UITheme.button("Attach");
        healthBtn.addActionListener(e -> attachFile(healthLabel));

        p.add(new JLabel("Health Declaration"));
        p.add(healthLabel);
        p.add(healthBtn);

        JLabel fitnessLabel = new JLabel("No file selected");
        JButton fitnessBtn = UITheme.button("Attach");
        fitnessBtn.addActionListener(e -> attachFile(fitnessLabel));

        p.add(new JLabel("Fitness to Work Clearance"));
        p.add(fitnessLabel);
        p.add(fitnessBtn);

        JLabel wellnessLabel = new JLabel("No file selected");
        JButton wellnessBtn = UITheme.button("Attach");
        wellnessBtn.addActionListener(e -> attachFile(wellnessLabel));

        p.add(new JLabel("Wellness Activity"));
        p.add(wellnessLabel);
        p.add(wellnessBtn);
        
        JPanel center=new JPanel(new BorderLayout());
        center.add(f,BorderLayout.NORTH);
        center.add(p,BorderLayout.CENTER);
        center.add(pwd,BorderLayout.SOUTH);
        add(center,BorderLayout.CENTER);
        save.addActionListener(e->save());
        change.addActionListener(e->change());
        load();
    }

private void add(JPanel p,String label,JTextField field) {
    p.add(new JLabel(label));
    p.add(field);
} 

private void attachFile(JLabel label) {

    JFileChooser chooser = new JFileChooser();

    int result = chooser.showOpenDialog(this);

    if (result == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        label.setText(file.getName());
    }
}

private void load(){
    try
    (Connection c=Database.getConnection();
    PreparedStatement p=c.prepareStatement("SELECT u.full_name,u.email,u.phone,e.address,e.emergency_contact FROM users u JOIN employees e ON u.id=e.user_id WHERE u.id=?"))
    {
        p.setInt(1,session.getId());
            try(ResultSet r=p.executeQuery()){
                if(r.next()){
                    full.setText(r.getString(1));
                    email.setText(r.getString(2));
                    phone.setText(r.getString(3));
                    address.setText(r.getString(4));
                    emergency.setText(r.getString(5));}
                }
            } catch(Exception ex){Dialogs.error(this,ex.getMessage());
        }
    }
private void save(){
    try(Connection c=Database.getConnection()){
        c.setAutoCommit(false);

        try(PreparedStatement u=c.prepareStatement("UPDATE users SET full_name=?,email=?,phone=? WHERE id=?");
            PreparedStatement e=c.prepareStatement("UPDATE employees SET address=?,emergency_contact=? WHERE user_id=?")) {
                u.setString(1,full.getText().trim());
                u.setString(2,email.getText().trim());
                u.setString(3,phone.getText().trim());
                u.setInt(4,session.getId());
                u.executeUpdate();
                e.setString(1,address.getText().trim());
                e.setString(2,emergency.getText().trim());
                e.setInt(3,session.getId());
                e.executeUpdate();c.commit();
                
                AuditService.log(session.getId(),"UPDATE","PROFILE","Updated employee profile.");
                Dialogs.info(this,"Profile saved.");
            } catch(Exception ex)
                {c.rollback();throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (Exception ex){Dialogs.error(this,ex.getMessage());
    }
}
private void change() { 
    try { 
        new AuthService().changePassword(session.getId(),
        new String(oldp.getPassword()),
        new String(newp.getPassword()));
        
        Dialogs.info(this,"Password changed.");
        oldp.setText("");
        newp.setText(""); 

    } catch (Exception ex) { 
        Dialogs.error(this,ex.getMessage());
    }
    }
}
