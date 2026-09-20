import javax.swing.*;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // todo в будущем будет вызов окна главного и соответственно запуск приложения
                //MainFrame mainW = new MainFrame();
                //mainW.setVisible(true);
            }
        });
    }
}