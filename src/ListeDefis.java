import javax.swing.*;
import javax.swing.plaf.SeparatorUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

// classe qui permet de lister les defis avec leur status et de lancer un defi ou bien de joindre un defi
public class ListeDefis {
    // hashmap qui contient les buttons et leur defi
    public HashMap buttons = new HashMap();
    // port de base
    private int portbase=22220;
    // port de defi
    private  int portdefi;
    // constructeur
    public ListeDefis(String name){
        // Create a new JFrame
        JFrame frame = new JFrame("List des defis");
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create a JPanel
        JPanel panel = new JPanel();
        // set color to  of panel
        panel.setBackground(new java.awt.Color(204, 255, 255));
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        // set to center of the panel every element
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        // set a border to every line
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // clear the list of defis and read the file
        Main.defis.clear();
        Main.lireContenuFichier();

        panel.add(new JLabel("vous pouvez lancer un defi            "));
        // retour a la ligne
        panel.add(new JSeparator());
        JButton buttonlancer = new JButton("Lancer");
        buttonlancer.setPreferredSize(new Dimension(100, 30));
        panel.add(buttonlancer);
        // listen sur ce button
        buttonlancer.addActionListener((ActionEvent e) -> {
            // get last line form the file for init the port of defi
            // every defi has a port
            if(Main.defis.size()>0) {
                String lastdefi = Main.defis.get(Main.defis.size() - 1);
                String[] parts = lastdefi.split(":");
                String name1 = parts[0];
                String port = parts[1];
                String status=parts[2];
                portdefi = Integer.parseInt(port) + 1;
            }
            else{
                // if there is no defi the port of defi is the port of base plus 1
                portdefi = ++portbase;
            }
            // store the defi in our file with the name and port and status
            Main.enregistrerDansFichier(name+":"+portdefi+":lanced");
            frame.dispose();
            // lancer le defi
            new XO(portdefi, name);
        });

        // si on a deja des defis dans notre fichier text on va les lister ici avec un boutton pour joindre sur chauque defi
        if(Main.defis.size()>0) {
            panel.add(new JLabel("                       ou bien vous pouvez Joindre un defi                               "));
            for (String defi : Main.defis) {
                String[] parts = defi.split(":");
                String name1 = parts[0];
                String port = parts[1];
                String status=parts[2];
                panel.add(new JLabel("                    Defi lance par :   " + name1));
                JButton button=null;
                if(status.equals("lanced"))
                  button= new JButton("Participer");
                else {
                    button = new JButton("En jeu");
                    // disable button
                    button.setEnabled(false);
                }
                button.setPreferredSize(new Dimension(100, 30));
                buttons.put(button, defi);
                panel.add(button);
                panel.add(new JLabel("                                 " ));
            }
            Set<JButton> keys1 = buttons.keySet();
            // ajouter un listener sur chaque boutton
            for (JButton key : keys1) {
                key.addActionListener((ActionEvent e) -> {
                    String defi = (String) buttons.get(key);
                    String[] parts = defi.split(":");
                    String name1 = parts[0];
                    String port = parts[1];
                    try {
                        new XO(Integer.parseInt(port),name,name1);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    frame.dispose();
                });
            }
        }


        // Set the JPanel as the content pane of the JFrame
        frame.setContentPane(panel);
        frame.setVisible(true);
    }
}
