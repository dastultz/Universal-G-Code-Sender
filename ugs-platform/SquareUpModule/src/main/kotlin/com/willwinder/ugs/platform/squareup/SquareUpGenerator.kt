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

import com.willwinder.universalgcodesender.model.UnitUtils
import java.io.PrintWriter
import java.lang.Math.ceil
import java.lang.Math.floor

/*
  @author dastultz
  Created on Feb 14, 2019
*/

class SquareUpGenerator(var settings: SquareUpSettings) {

    fun unitConverter(value: Double): Double {
        val unitScaleFactor = UnitUtils.scaleUnits(settings.units, UnitUtils.Units.MM)
        return value * unitScaleFactor
    }

    fun generate(output: PrintWriter) {
        val generator = GCodeGenerator(output)
        val turtle = Turtle(generator, settings.feed)
        with(generator) {
            begin(settings.units)
            planeXY()
            spindleOn()
        }

        when (settings.operation) {
            Operation.MILL_FACING -> millFacing(turtle)
            Operation.TURN_FACING -> turnFacing(turtle)
        }

        generator.end()
    }

    private fun turnFacing(turtle: Turtle) = with(turtle) {
        val scaler = UnitUtils.scaleUnits(UnitUtils.Units.MM, settings.units) // scale literal distances to user units
        val clearance = settings.safetyHeight
        val stockRadius = settings.stockDiameter / 2
        val retractGap = scaler * 5 // 5 mm safety gap
        val retractX = stockRadius + retractGap
        val approachGap = scaler * 3
        val entryX = stockRadius + approachGap
        val center = 0 - settings.crossCenter

        val stepDown =
            if (settings.maxStepDown > settings.totalStepDown) settings.totalStepDown
            else settings.maxStepDown
        val totalFullPasses = floor(settings.totalStepDown / stepDown).toInt()

        rapid(x = clearance, z = approachGap)
        rapid(x = retractX)
        fun faceLayer(z: Double) {
            feedAbsolute(x = entryX, z = z)
            feedAbsolute(x = center) // feed into center
            rapid(x = center + approachGap, z = z + approachGap)
            rapid(x = retractX)
        }

        var z = 0.0
        for (layer in 1..totalFullPasses) {
            z -= stepDown
            faceLayer(z)
        }
        // do another pass if we aren't all the way in yet
        val finalZ = settings.totalStepDown
        if (z > finalZ) faceLayer(finalZ)

        rapid(x = clearance, z = approachGap)
    }

    private fun millFacing(turtle: Turtle) = with(turtle) {
        val xEntryClearance = settings.bitDiameter
        val xEntry = settings.stockWidth + xEntryClearance
        val bitRadius = settings.bitDiameter / 2
        val yEntry = 0 - bitRadius + settings.stepOver
        val arcDiameter = settings.stepOver
        val arcWidth = bitRadius
        val feedWidth = settings.stockWidth - (arcWidth * 2)

        val totalHorizontalPasses = ceil(settings.stockDepth / settings.stepOver).toInt()

        val stepDown =
            if (settings.maxStepDown > settings.totalStepDown) settings.totalStepDown
            else settings.maxStepDown
        val totalFullVerticalPasses = floor(settings.totalStepDown / stepDown).toInt()

        fun faceLayer(z: Double) {
            // drop in and orient
            rapid(x = xEntry, y = yEntry)
            rapid(z = z)
            faceTo(Heading.LEFT)

            // first pass
            feed(feedWidth + xEntryClearance + arcWidth)
            // remaining passes
            for (i in 1 until totalHorizontalPasses) {
                if (i % 2 == 1) {
                    arcRight(arcDiameter)
                    arcRight(arcDiameter)
                } else {
                    arcLeft(arcDiameter)
                    arcLeft(arcDiameter)
                }
                feed(feedWidth)
            }
            // move off the stock
            feed(xEntryClearance + arcWidth)

            // back to safe height
            rapid(z = settings.safetyHeight)
        }

        safeHeight(settings.safetyHeight)

        var z = 0.0
        for (layer in 1..totalFullVerticalPasses) {
            z -= stepDown
            faceLayer(z)
        }
        // do another pass if we aren't all the way down yet
        val finalZ = 0 - settings.totalStepDown
        if (z > finalZ) faceLayer(finalZ)
    }
}
