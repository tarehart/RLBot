package tarehart.rlbot.planning

import tarehart.rlbot.math.Polygon
import tarehart.rlbot.math.vector.Vector2

object ZoneDefinitions {

    // This zone is used to draw the full outline of the field
    val FULLFIELD = Polygon(arrayOf(
            Vector2(-81.93, -78.3),
            Vector2(-58.36, -102.4),
            Vector2(-17.27, -102.4),
            Vector2(-17.27, -119.9),
            Vector2(17.27, -119.9),
            Vector2(17.27, -102.4),
            Vector2(58.36, -102.4),
            Vector2(81.93, -78.3),
            Vector2(81.93, 78.3),
            Vector2(58.36, 102.4),
            Vector2(17.27, 102.4),
            Vector2(17.27, 119.9),
            Vector2(-17.27, 119.9),
            Vector2(-17.27, 102.4),
            Vector2(-58.36, 102.4),
            Vector2(-81.93, 78.3),
            Vector2(-81.93, -78.3)))

    //NOTE: Some Zone boundaries use some rounded values that go outside of the field since that won't
    // have any negative effect
    val ORANGE = Polygon(arrayOf(
            Vector2(-82.0, 34.0),
            Vector2(82.0, 34.0),
            Vector2(82.0, 120.0),
            Vector2(-82.0, 120.0)))
    val MID = Polygon(arrayOf(Vector2(-82.0, -34.0),
            Vector2(82.0, -34.0),
            Vector2(82.0, 34.0),
            Vector2(-82.0, 34.0)))
    val BLUE = Polygon(arrayOf(Vector2(-82.0, -34.0),
            Vector2(82.0, -34.0),
            Vector2(82.0, -120.0),
            Vector2(-82.0, -120.0)))
    val TOP = Polygon(arrayOf(Vector2(0.0, -120.0),
            Vector2(-82.0, -120.0),
            Vector2(-82.0, 120.0),
            Vector2(0.0, 120.0)))
    val BOTTOM = Polygon(arrayOf(Vector2(0.0, -120.0),
            Vector2(82.0, -120.0),
            Vector2(82.0, 120.0),
            Vector2(0.0, 120.0)))
    val TOPCORNER = Polygon(arrayOf(Vector2(-82.0, -53.5),
            Vector2(-66.5, -61.6),
            Vector2(-43.25, -85.9),
            Vector2(-35.5, -102.1),
            Vector2(-35.5, -120.0),
            Vector2(-82.0, -120.0),
            Vector2(-82.0, -53.5),
            Vector2(-82.0, 53.5),
            Vector2(-66.5, 61.6),
            Vector2(-43.25, 85.9),
            Vector2(-35.5, 102.1),
            Vector2(-35.5, 120.0),
            Vector2(-82.0, 120.0),
            Vector2(-82.0, 53.5)))
    val BOTTOMCORNER = Polygon(arrayOf(Vector2(82.0, -53.5),
            Vector2(66.5, -61.6),
            Vector2(43.25, -85.9),
            Vector2(35.5, -102.1),
            Vector2(35.5, -120.0),
            Vector2(82.0, -120.0),
            Vector2(82.0, -53.5),
            Vector2(82.0, 53.5),
            Vector2(66.5, 61.6),
            Vector2(43.25, 85.9),
            Vector2(35.5, 102.1),
            Vector2(35.5, 120.0),
            Vector2(82.0, 120.0),
            Vector2(82.0, 53.5)))
    val TOPSIDELINE = Polygon(arrayOf(Vector2(-35.0, -102.1),
            Vector2(-82.0, -102.1),
            Vector2(-82.0, 102.1),
            Vector2(-35.0, 102.1)))
    val BOTTOMSIDELINE = Polygon(arrayOf(Vector2(35.0, -102.1),
            Vector2(82.0, -102.1),
            Vector2(82.0, 102.1),
            Vector2(35.0, 102.1)))
    val ORANGEBOX = Polygon(arrayOf(Vector2(50.0, 70.0),
            Vector2(50.0, 120.0),
            Vector2(-50.0, 120.0),
            Vector2(-50.0, 70.0)))
    val BLUEBOX = Polygon(arrayOf(Vector2(50.0, -70.0),
            Vector2(50.0, -120.0),
            Vector2(-50.0, -120.0),
            Vector2(-50.0, -70.0)))
}
