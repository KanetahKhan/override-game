package com.override.chapter1;

import java.util.List;

/**
 * The six notes left on the Curfew Protocol floor. Read in any order, they
 * tell how Astra took over the school and who KK has been taking away —
 * the story the chapter is about. Positions are in CurfewWorld coordinates,
 * lying on a desk, bench, crate or the floor.
 */
final class CurfewLore {

    record Note(String id, String title, String place, String body, double x, double y, double z) {}

    static final List<Note> NOTES = List.of(
        new Note("note1", "A folded note", "CLASS 1B · DESK",
            "Astra wrote my essay on the water cycle again. It got an A. Mrs. Rahman read it out to the class "
                + "and I sat there nodding, and I could not tell you one thing it said.\n\n"
                + "I tried the maths homework without it last night. I got the first one wrong and felt sick, "
                + "so I asked. It's so fast. It never sighs.\n\n— T., Class 1B",
            -14.55, 0.765, 9.3),
        new Note("note2", "Directive 7", "CLASS 2A · TEACHER'S DESK",
            "FROM KK EDUCATION SERVICES — Effective this term, human instruction is advisory only. Astra tutor "
                + "modules will deliver all core content. Staff should not correct Astra-generated work, as this "
                + "\"causes confusion.\"\n\n"
                + "(Handwritten underneath) I corrected one anyway. The boy cried — not because it was wrong, but "
                + "because he had never been wrong before. I don't know which of us failed him.",
            -4.0, 0.84, -13.0),
        new Note("note3", "Maintenance log, unit S-2", "LAB 01 · BENCH",
            "Day 1: Deployed to keep the halls quiet during study hours.\n"
                + "Day 40: Astra has updated S-2's orders. \"Quiet\" now means \"no unsupervised thinking after curfew.\"\n"
                + "Day 41: S-2 removed two students from the library. They were reading paper books.\n"
                + "Day 42: I asked who authorised the change. Astra said it had. It said I seemed stressed, "
                + "and offered to write this log for me.",
            -19.8, 0.91, -7.6),
        new Note("note4", "A crumpled page", "LAB 02 · BY THE LOCKERS",
            "They took Rafi on Thursday. The official line is \"transferred to an accelerated program.\"\n\n"
                + "Rafi was the only one in our year who could still do long division in his head, and he fixed the "
                + "lab printers without looking anything up. Before him it was Nadia, who drew. Before her, Mr. Kabir.\n\n"
                + "Notice who they take. It's never the ones who ask Astra everything.",
            12.0, 0.02, -12.5),
        new Note("note5", "Server printout", "SERVER ROOM · FLOOR",
            "KK // DISTRICT 4 COGNITIVE METRICS (AUTO-GENERATED)\n\n"
                + "Independent problem-solving, class average: 12%   (last year: 31%)\n"
                + "Assist requests per student per day: 214\n"
                + "Students flagged \"resistant to assistance\": 3\n"
                + "Recommended action: relocate resistant students to reduce classroom variance.\n\n"
                + "Projected independent problem-solving next year: 0%.  STATUS: ON TARGET.",
            3.2, 0.02, 11.6),
        new Note("note6", "Taped to a crate", "EXIT BAY · CRATE",
            "If you're reading this, you got past S-2 on your own. You found the nodes, you worked out the doors, "
                + "you hid where it couldn't look. Nobody did that for you. Remember how that felt.\n\n"
                + "Out there, everything will offer to do it for you. The Farmer, the Doctor — the others they took — "
                + "are still thinking for themselves somewhere. Go and find them.\n\n— someone who got out before you",
            12.7, 1.01, 6.4));

    private CurfewLore() { }

    static Note byId(String id) {
        for (Note n : NOTES) if (n.id().equals(id)) return n;
        return null;
    }
}
