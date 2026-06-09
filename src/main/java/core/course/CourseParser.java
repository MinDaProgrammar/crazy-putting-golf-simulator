package core.course;

import javax.swing.*;

import java.awt.*;
import java.io.*;

/**
 * Utility class for parsing {@link Course} data from various character sources.
 * <p>
 * A {@code CourseParser} reads key–value pairs from a textual description of a
 * golf course. Each non-empty, non-comment line must contain an assignment of
 * the form:
 * </p>
 *
 * <pre>
 * key = value
 * </pre>
 *
 * <p>
 * Supported keys include:
 * <ul>
 *   <li>{@code height_profile}</li>
 *   <li>{@code friction_kinetic}</li>
 *   <li>{@code friction_static}</li>
 *   <li>{@code target_x}, {@code target_y}, {@code target_r}</li>
 *   <li>{@code start_x}, {@code start_y}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Lines beginning with {@code #} are treated as comments and ignored.
 * The special line {@code end} terminates parsing early.
 * </p>
 *
 * <p>
 * This parser is source-agnostic: it can read from files, strings,
 * network streams, or interactive user input via {@link System#in}.
 * </p>
 */
public class CourseParser {
    
    /**
     * Extracts the value part of a {@code key = value} line.
     *
     * @param line a non-null line containing an '=' character
     * @return the trimmed value to the right of '='
     */
    private static String extractValue(String line) {
        return line.split("=", 2)[1].trim();
    }

    /**
     * Parses a {@link Course} description from the given {@link Reader}.
     * <p>
     * The method reads line by line until:
     * <ul>
     *   <li>the end of the stream is reached, or</li>
     *   <li>a line containing only {@code end} (case-insensitive) is encountered.</li>
     * </ul>
     * </p>
     *
     * <p>
     * This method does not close the provided reader; callers are responsible
     * for managing the reader's lifecycle.
     * </p>
     *
     * @param reader the character source to read from
     * @return a populated {@link Course} instance
     * @throws IOException if an I/O error occurs while reading
     */
    private static Course parse(Reader reader) throws IOException {
        Course data = new Course();
        BufferedReader br = new BufferedReader(reader);

        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();

            // Stop condition for interactive input
            if (line.equalsIgnoreCase("end")) break;

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("height_profile")) {
                data.setHeightProfile(extractValue(line));
            }
            else if (line.startsWith("friction_kinetic")) {

                data.setFrictionKinetic(Double.parseDouble(extractValue(line)));
            }
            else if (line.startsWith("friction_static")) {
                data.setFrictionStatic(Double.parseDouble(extractValue(line)));
            }
            else if (line.startsWith("target_x")) {
                data.setTargetX(Double.parseDouble(extractValue(line)));
            }
            else if (line.startsWith("target_y")) {
                data.setTargetY(Double.parseDouble(extractValue(line)));
            }
            else if (line.startsWith("target_r")) {
                data.setTargetRadius(Double.parseDouble(extractValue(line)));
            }
            else if (line.startsWith("start_x")) {
                data.setStartX(Double.parseDouble(extractValue(line)));
            }
            else if (line.startsWith("start_y")) {
                data.setStartY(Double.parseDouble(extractValue(line)));
            }
            // Tree and sandpit parsing intentionally omitted for now
        }
        br.close();
        return data;
    }

    /**
     * Parses a {@link Course} from a file located at the given path.
     *
     * @param path the filesystem path to the course description file
     * @return a populated {@link Course} instance
     * @throws IOException if the file cannot be read
     */
    public static Course courseFromFile(String path) throws IOException {
        try (Reader r = new FileReader(path)) {
            return CourseParser.parse(r);
        }
    }

    /**
     * Parses a {@link Course} from interactive user input.
     * <p>
     * The user may type lines into the console. Parsing stops when the user
     * enters a line containing only {@code end} (case-insensitive), or when
     * an EOF signal is sent:
     * </p>
     *
     * <ul>
     *   <li>Linux/macOS: press {@code CTRL + D}</li>
     *   <li>Windows: press {@code CTRL + Z} then Enter</li>
     * </ul>
     *
     * @return a populated {@link Course} instance
     * @throws IOException if an I/O error occurs while reading
     */
    public static Course courseFromUserInput() throws IOException {
        JTextField heightProfileField = new JTextField();
        JTextField frictionKineticField = new JTextField();
        JTextField frictionStaticField = new JTextField();
        JTextField startXField = new JTextField();
        JTextField startYField = new JTextField();
        JTextField targetXField = new JTextField();
        JTextField targetYField = new JTextField();
        JTextField targetRadiusField = new JTextField();
        JTextField vMaxField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

        panel.add(new JLabel("Height Profile:"));
        panel.add(heightProfileField);

        panel.add(new JLabel("Friction Kinetic:"));
        panel.add(frictionKineticField);

        panel.add(new JLabel("Friction Static:"));
        panel.add(frictionStaticField);

        panel.add(new JLabel("Start X:"));
        panel.add(startXField);

        panel.add(new JLabel("Start Y:"));
        panel.add(startYField);

        panel.add(new JLabel("Target X:"));
        panel.add(targetXField);

        panel.add(new JLabel("Target Y:"));
        panel.add(targetYField);

        panel.add(new JLabel("Target Radius:"));
        panel.add(targetRadiusField);

        panel.add(new JLabel("Vmax:"));
        panel.add(vMaxField);

        int result = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Enter Course Parameters",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            Course course = new Course();
            course.setHeightProfile(heightProfileField.getText());
            course.setFrictionKinetic(Double.parseDouble(frictionKineticField.getText()));
            course.setFrictionStatic(Double.parseDouble(frictionStaticField.getText()));
            course.setStartX(Double.parseDouble(startXField.getText()));
            course.setStartY(Double.parseDouble(startYField.getText()));
            course.setTargetX(Double.parseDouble(targetXField.getText()));
            course.setTargetY(Double.parseDouble(targetYField.getText()));
            course.setTargetRadius(Double.parseDouble(targetRadiusField.getText()));
            course.setVMax(Double.parseDouble(vMaxField.getText()));
            return course;
        }
        return null; // user canceled
    }

    public static Course createCourseFromUserParameter(String heightProfile,
                                                       double kinetic_friction,
                                                       double static_friction,
                                                       double target_x,
                                                       double target_y,
                                                       double target_radius,
                                                       double start_x,
                                                       double start_y,
                                                       double vmax) throws IOException {
        Course course = new Course();
        course.setHeightProfile(heightProfile);
        course.setFrictionKinetic(kinetic_friction);
        course.setFrictionStatic(static_friction);
        course.setTargetX(target_x);
        course.setTargetY(target_y);
        course.setTargetRadius(target_radius);
        course.setStartX(start_x);
        course.setStartY(start_y);
        course.setVMax(vmax);
        return course;
    }

    public static Course chooseCourse() throws IOException{

        String[] options = {"Load from file", "Enter manually", "Cancel"};

        int choice = JOptionPane.showOptionDialog(
                null,
                "How would you like to load the course?",
                "Course Loader",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice == 0){
            // Load from file
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("default directory or directory of courses"));// modify here 
            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                return CourseParser.courseFromFile(file.getAbsolutePath());
            }
        }
        else if (choice == 1){
            // Load from user input dialog
            return CourseParser.courseFromUserInput();
        }
        return null; // user canceled
    }
}



