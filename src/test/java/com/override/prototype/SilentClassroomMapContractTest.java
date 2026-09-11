package com.override.prototype;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilentClassroomMapContractTest {

    private static final String MAP_RESOURCE =
            "assets/levels/academic-building-2-floor-2-v2.tmx";
    private static final Pattern NUMBERED_ROOM = Pattern.compile("^Room\\s+(30[1-7])\\b");

    private static Document map;

    @BeforeAll
    static void parseMap() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        ClassLoader loader = SilentClassroomMapContractTest.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream(MAP_RESOURCE)) {
            assertNotNull(input, "Missing classpath resource " + MAP_RESOURCE);
            map = factory.newDocumentBuilder().parse(input);
        }
    }

    @Test
    void containsExactlySevenNumberedClassroomZones() {
        List<Element> numberedZones = objectsOfType("RoomZone").stream()
                .filter(object -> NUMBERED_ROOM.matcher(object.getAttribute("name")).find())
                .toList();
        Set<String> roomNumbers = new HashSet<>();
        for (Element zone : numberedZones) {
            Matcher matcher = NUMBERED_ROOM.matcher(zone.getAttribute("name"));
            assertTrue(matcher.find());
            roomNumbers.add(matcher.group(1));
        }

        assertEquals(7, numberedZones.size());
        assertEquals(Set.of("301", "302", "303", "304", "305", "306", "307"), roomNumbers);
    }

    @Test
    void corridorHasNoArcadeTerminalsAtStart() {
        assertEquals(0, objectsOfType("Arcade").size());
    }

    @Test
    void roomMapsExposeSixArcadeSearchSlots() throws Exception {
        Set<String> classroomSlots = arcadeSpawnSlotIds(
                "assets/levels/iut-classroom.tmx"
        );
        Set<String> ictLabSlots = arcadeSpawnSlotIds(
                "assets/levels/iut-ict-lab.tmx"
        );

        assertEquals(
                Set.of("classroom-1", "classroom-2", "classroom-3"),
                classroomSlots
        );
        assertEquals(
                Set.of("ictlab-1", "ictlab-2", "ictlab-3"),
                ictLabSlots
        );
    }

    private static Set<String> arcadeSpawnSlotIds(String resource) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        ClassLoader loader = SilentClassroomMapContractTest.class.getClassLoader();
        Document roomMap;
        try (InputStream input = loader.getResourceAsStream(resource)) {
            assertNotNull(input, "Missing classpath resource " + resource);
            roomMap = factory.newDocumentBuilder().parse(input);
        }

        NodeList objects = roomMap.getElementsByTagName("object");
        Set<String> slotIds = new HashSet<>();
        for (int i = 0; i < objects.getLength(); i++) {
            Element object = (Element) objects.item(i);
            if ("ArcadeSpawn".equals(object.getAttribute("type"))) {
                slotIds.add(property(object, "slotId"));
            }
        }
        return slotIds;
    }

    @Test
    void allSevenClassroomDoorsAreLocal() {
        List<Element> doors = objectsOfType("Door");

        assertEquals(7, doors.size());
        for (Element door : doors) {
            assertEquals("true", property(door, "localDoor"), door.getAttribute("name"));
        }
    }

    @Test
    void sentinel01HasOneActiveSpawnLinkedToOnePatrolPath() {
        List<Element> spawns = namedObjects("EnemySpawn", "Sentinel01Spawn");
        List<Element> paths = namedObjects("PatrolPath", "Sentinel01Path");

        assertEquals(1, spawns.size());
        assertEquals("Sentinel01Path", property(spawns.getFirst(), "path"));
        assertEquals(1, paths.size());

        NodeList polylines = paths.getFirst().getElementsByTagName("polyline");
        assertEquals(1, polylines.getLength());
        assertFalse(((Element) polylines.item(0)).getAttribute("points").isBlank());
    }

    @Test
    void containsBreakerProfessorAndThreeGameExit() {
        List<Element> breakers = namedObjects("Breaker", "ServerBreaker");
        List<Element> professors = namedObjects("Dialogue", "Professor");
        List<Element> exits = objectsOfType("Exit");

        assertEquals(1, breakers.size());
        assertEquals(1, professors.size());
        assertEquals(1, exits.size());
        assertEquals("3", property(exits.getFirst(), "requiredGames"));
    }

    private static List<Element> namedObjects(String type, String name) {
        return objectsOfType(type).stream()
                .filter(object -> name.equals(object.getAttribute("name")))
                .toList();
    }

    private static List<Element> objectsOfType(String type) {
        NodeList objects = map.getElementsByTagName("object");
        List<Element> matches = new ArrayList<>();
        for (int i = 0; i < objects.getLength(); i++) {
            Element object = (Element) objects.item(i);
            if (type.equals(object.getAttribute("type"))) {
                matches.add(object);
            }
        }
        return matches;
    }

    private static String property(Element object, String propertyName) {
        NodeList children = object.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element properties)
                    || !"properties".equals(properties.getTagName())) {
                continue;
            }

            NodeList candidates = properties.getElementsByTagName("property");
            for (int j = 0; j < candidates.getLength(); j++) {
                Element candidate = (Element) candidates.item(j);
                if (propertyName.equals(candidate.getAttribute("name"))) {
                    String attributeValue = candidate.getAttribute("value");
                    return attributeValue.isEmpty()
                            ? candidate.getTextContent().trim()
                            : attributeValue;
                }
            }
        }
        throw new AssertionError(
                "Object " + object.getAttribute("name")
                        + " has no property " + propertyName
        );
    }
}
