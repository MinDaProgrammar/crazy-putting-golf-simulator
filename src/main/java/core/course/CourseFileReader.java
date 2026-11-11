package core.course;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import core.course.Course.Sandpit;
import core.course.Course.Tree;

public class CourseFileReader {

    public static Course readCourseFile(String filePath) throws IOException, IllegalArgumentException {
        Course course = new Course();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                int commentIndex = line.indexOf('#');
                if (commentIndex >= 0) {
                    line = line.substring(0, commentIndex).trim();
                }

                // Ignore empty and comment-only lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("=", 2); // we split by = sign

                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid line format: " + line);
                }

                // We format the strings correctly, then use setters to define the course.
                String key = parts[0].trim().toLowerCase();
                String value = parts[1].trim();
                switch (key) {
                    case "height_profile": course.setHeightProfile(value); break;
                    case "friction_kinetic": course.setFrictionKinetic(Double.parseDouble(value)); break;
                    case "friction_static": course.setFrictionStatic(Double.parseDouble(value)); break;
                    case "start_x": course.setStartX(Double.parseDouble(value)); break;
                    case "start_y": course.setStartY(Double.parseDouble(value)); break;
                    case "target_x": course.setTargetX(Double.parseDouble(value)); break;
                    case "target_y": course.setTargetY(Double.parseDouble(value)); break;
                    case "target_radius": course.setTargetRadius(Double.parseDouble(value)); break;
                    case "vmax": course.setVMax(Double.parseDouble(value)); break;
                    case "tree":
                        // Strip parentheses if present, then split by comma.
                        String cleanTreeStr = value.replace("(", "").replace(")", "");
                        String[] tParts = cleanTreeStr.split(",");
                        if (tParts.length == 3) {
                            course.addTree(new Tree(
                                    Double.parseDouble(tParts[0].trim()),
                                    Double.parseDouble(tParts[1].trim()),
                                    Double.parseDouble(tParts[2].trim())
                            ));
                        } else {
                            throw new IllegalArgumentException("Tree must have format: x, y, radius");
                        }
                        break;

                    case "sandpit":
                        String cleanSandStr = value.replace("(", "").replace(")", "");
                        String[] sParts = cleanSandStr.split(",");
                        if (sParts.length == 5) {
                            course.addSandpit(new Sandpit(
                                    Double.parseDouble(sParts[0].trim()),
                                    Double.parseDouble(sParts[1].trim()),
                                    Double.parseDouble(sParts[2].trim()),
                                    Double.parseDouble(sParts[3].trim()),
                                    Double.parseDouble(sParts[4].trim())
                            ));
                        } else if (sParts.length == 4) {
                            double friction = Double.parseDouble(sParts[3].trim());
                            course.addSandpit(new Sandpit(
                                    Double.parseDouble(sParts[0].trim()),
                                    Double.parseDouble(sParts[1].trim()),
                                    Double.parseDouble(sParts[2].trim()),
                                    friction,
                                    friction
                            ));
                        } else {
                            throw new IllegalArgumentException("Sandpit must have format: x, y, radius, muK[, muS]");
                        }
                        break;
                    default:
                        System.out.println("Warning: Unknown configuration key '" + key + "'");
                }
            }
        }

        return course;
    }
}
