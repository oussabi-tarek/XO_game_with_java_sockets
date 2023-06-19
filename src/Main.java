import java.io.*;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// classe principale qui contient la méthode main
public class Main {
    // une liste des défis qui sont enregistrés dans un fichier texte "defis.txt"
    // le "defis.txt" est situé dans le dossier "src" du projet permet
    // d enregistrer les défis avec leur statut (En attente, En jeu) , si un defi a fini on le supprime du fichier
    public static ArrayList<String> defis=new ArrayList<>();
    // chemin du fichier "defis.txt"
    private static String filePath="C:\\Users\\AdMin\\IdeaProjects\\XO game\\src\\defis.txt";
    public static void main(String[] args) throws SQLException, UnknownHostException {

        lireContenuFichier();
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new Connexion().setVisible(true);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    // methode qui permet d'enregistrer un defi dans le fichier "defis.txt"
    public static void enregistrerDansFichier(String contenu) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\AdMin\\IdeaProjects\\XO game\\src\\defis.txt", true))) {
            writer.write(contenu);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // methode qui permet de lire le contenu du fichier "defis.txt" et de l enregistrer dans la liste "defis"
    public static void lireContenuFichier() {
        StringBuilder contenu = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\AdMin\\IdeaProjects\\XO game\\src\\defis.txt"))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                contenu.append(ligne).append("\n");
                defis.add(ligne);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // methode qui permet de supprimer un defi du fichier "defis.txt" quand il est fini
    public static void deleteLineFromFile(String indiceline){
        int lineIndex=0;
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("line: "+line);
                lines.add(line);
                if(line.contains(indiceline)){
                    lineIndex=lines.indexOf(line);
                    System.out.println("lineinice: "+lineIndex);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("line index: "+lineIndex);

        // Check if the line index is valid
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            System.out.println("Invalid line index");
            return;
        }

        // Remove the desired line from the list
        lines.remove(lineIndex);

        // Write the modified list back to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // methode qui permet de modifier le statut d un defi dans le fichier "defis.txt" quand il est en jeu
    public  static void modifyStatus(String indiceline){
        int lineIndex=0;
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("line: "+line);
                lines.add(line);
                if(line.contains(indiceline)){
                    lineIndex=lines.indexOf(line);
                    String parts[]=line.split(":");
                    String name1=parts[0];
                    String port1=parts[1];
                    String status=parts[2];
                    line=name1+":"+port1+":"+"En jeu";
                    lines.remove(lines.size()-1);
                    lines.add(line);
                    System.out.println("lineinice: "+lineIndex);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("line index: "+lineIndex);

        // Check if the line index is valid
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            System.out.println("Invalid line index");
            return;
        }



        // Write the modified list back to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
