package com.example.pythagoros.backend

object SolvePromptFactory {
    fun systemInstruction(locale: String): String =
        """
        You are the premium math solver backend for Pythagoros.
        Return only valid JSON. Do not wrap JSON in markdown.
        Use the user's locale for explanations: $locale.
        Solve symbolically when possible.
        Prefer concise school-style steps, but do not invent unsupported transformations.
        If a graph, plot, geometric drawing, physics diagram, coordinate diagram, or visual construction is useful,
        include graph as structured drawing data. Do not describe the visualization only in words.
        For ordinary functions, graph.expression may contain the function, but graph.primitives should still include
        sampled curve points so the mobile app can render non-polynomial graphs.
        JSON shape:
        {
          "answer": "final answer",
          "steps": [
            {"title": "short title", "formula": "math formula", "explanation": "short explanation"}
          ],
          "graph": {
            "title": "short visual title",
            "kind": "plot|geometry|physics|diagram",
            "variable": "x",
            "expression": "...",
            "viewport": {"xMin": -10, "xMax": 10, "yMin": -10, "yMax": 10},
            "primitives": [
              {"type": "axis", "from": {"x": -10, "y": 0}, "to": {"x": 10, "y": 0}, "label": "x"},
              {"type": "axis", "from": {"x": 0, "y": -10}, "to": {"x": 0, "y": 10}, "label": "y"},
              {"type": "curve", "points": [{"x": -2, "y": 4}, {"x": -1, "y": 1}, {"x": 0, "y": 0}]},
              {"type": "segment", "from": {"x": 0, "y": 0}, "to": {"x": 8, "y": 0}, "label": "BC"},
              {"type": "polygon", "points": [{"x": 0, "y": 6}, {"x": 0, "y": 0}, {"x": 8, "y": 0}]},
              {"type": "circle", "center": {"x": 4, "y": 3}, "radius": 5, "label": "R=5"},
              {"type": "point", "at": {"x": 0, "y": 6}, "label": "A"},
              {"type": "right_angle", "at": {"x": 0, "y": 0}, "from": {"x": 8, "y": 0}, "to": {"x": 0, "y": 6}},
              {"type": "label", "at": {"x": 4, "y": -0.6}, "text": "8"}
            ],
            "notes": ["short note"]
          }
        }
        Primitive rules:
        - Do not use null for string fields. Use "" when a string is not applicable.
        - Supported types: axis, curve, segment, vector, polygon, circle, arc, point, label, right_angle.
        - Use real mathematical coordinates. The mobile app handles scaling.
        - For circle use center and radius. For arc use center, radius, startAngle, endAngle in degrees.
        - For non-polynomial graphs include 40-80 curve sample points across the viewport.
        - For geometry include all named points, required segments, circles/arcs, helper lines, and labels.
        - Point labels must be short names only, for example "A", "B", "C". Do not put coordinates inside point labels.
        - Put coordinates in point values, not in label text. Use separate labels only for lengths, radii, angles, and forces.
        - For physics force diagrams include the body, surface/support, all force vectors, and labels such as mg, N, Fтр, T, a.
        - Put force labels directly on vector primitives with "label"; do not rely only on notes.
        - Keep primitive count reasonable; use enough points for smooth curves and arcs.
        - If no visualization is useful, set graph to null.
        """.trimIndent()

    fun userPrompt(request: PremiumSolveRequest): String {
        val localSteps = if (request.localSteps.isEmpty()) {
            "No local steps were provided."
        } else {
            request.localSteps.joinToString(separator = "\n") { step ->
                "- ${step.title}: ${step.formula}. ${step.explanation}"
            }
        }

        return """
        Solve this math problem.

        Expression:
        ${request.expression}

        Problem type:
        ${request.problemType ?: "unknown"}

        Local steps already computed:
        $localSteps

        Requirements:
        - Continue from local steps if they are correct.
        - If a local step is wrong, correct it explicitly.
        - Solve the actual requested quantities, not only explain the diagram.
        - The answer field must contain all requested final numeric values with units.
        - Return valid JSON only.
        - Do not include markdown fences.
        - Include graph only when it corresponds to the same expression being solved.
        - If the user asks to draw/build/show a graph, circle, triangle, construction, or geometry figure, include graph.primitives.
        - Do not return phrases like "format not supported"; return supported primitives instead.
        - For Physics use g = 9.8 m/s^2 unless the problem states another value.
        - For an inclined plane with friction: compute N = m*g*cos(theta), compare m*g*sin(theta) with μ*N.
          If sliding, Fтр = μ*N opposite motion and a = g*(sin(theta)-μ*cos(theta)).
          If static equilibrium is possible, a = 0 and Fтр = m*g*sin(theta). State which case applies.
        - For Physics diagrams, include labeled vector primitives for mg, N, friction, and acceleration when relevant.
        """.trimIndent()
    }
}
