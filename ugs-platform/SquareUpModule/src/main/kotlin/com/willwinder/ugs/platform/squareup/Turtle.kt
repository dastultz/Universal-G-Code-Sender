/*
    Copyprintln 2017 Will Winder

    This file is part of Universal Gcode Sender (UGS).

    UGS is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UGS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UGS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.willwinder.ugs.platform.squareup

/*
  @author dastultz
  Created on Mar 13, 2019
*/

enum class Heading {
    LEFT, RIGHT, IN, OUT
}

/*
From the old days of "Turtle Graphics" programming.
 */
class Turtle(
    private val generator: GCodeGenerator,
    feed: Double
) {

    private var heading = Heading.RIGHT

    init {
        generator.feedAbsolute(feed = feed)
    }

    fun faceTo(heading: Heading) {
        this.heading = heading
    }

    fun safeHeight(z: Double) {
        generator.rapid(z = z)
    }

    fun rapid(x: Double? = null, y: Double? = null, z: Double? = null) {
        generator.rapid(x, y, z)
    }

    fun feed(distance: Double, feed: Double? = null) {
        when (heading) {
            Heading.RIGHT -> generator.feedRelative(x = distance, feed = feed)
            Heading.LEFT -> generator.feedRelative(x = 0 - distance, feed = feed)
            Heading.IN -> generator.feedRelative(y = distance, feed = feed)
            Heading.OUT -> generator.feedRelative(y = 0 - distance, feed = feed)
        }
    }

    fun turnRight() {
        when (heading) {
            Heading.RIGHT -> heading = Heading.OUT
            Heading.LEFT -> heading = Heading.IN
            Heading.IN -> heading = Heading.RIGHT
            Heading.OUT -> heading = Heading.LEFT
        }
    }

    fun turnLeft() {
        when (heading) {
            Heading.RIGHT -> heading = Heading.IN
            Heading.LEFT -> heading = Heading.OUT
            Heading.IN -> heading = Heading.LEFT
            Heading.OUT -> heading = Heading.RIGHT
        }
    }

    fun arcRight(diameter: Double) {
        val radius = diameter / 2
        var i = 0.0
        var j = 0.0
        var x = 0.0
        var y = 0.0
        when (heading) {
            Heading.LEFT -> {
                x = 0 - radius
                j = radius
                y = radius
            }
            Heading.RIGHT -> {
                x = radius
                j = 0 - radius
                y = 0 - radius
            }
            Heading.IN -> {
                i = radius
                x = radius
                y = radius
            }
            Heading.OUT -> {
                i = 0 - radius
                x = 0 - radius
                y = 0 - radius
            }
        }
        turnRight()
        generator.clockWiseArcRel(x = x, y = y, i = i, j = j)
    }

    fun arcLeft(diameter: Double) {
        val radius = diameter / 2
        var i = 0.0
        var j = 0.0
        var x = 0.0
        var y = 0.0
        when (heading) {
            Heading.LEFT -> {
                x = 0 - radius
                j = 0 - radius
                y = 0 - radius
            }
            Heading.RIGHT -> {
                x = radius
                j = radius
                y = radius
            }
            Heading.IN -> {
                y = radius
                i = 0 - radius
                x = 0 - radius
            }
            Heading.OUT -> {
                y = 0 - radius
                i = radius
                x = radius
            }
        }
        turnLeft()
        generator.counterClockWiseArcRel(x = x, y = y, i = i, j = j)
    }
}