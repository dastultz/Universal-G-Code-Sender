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

import com.willwinder.universalgcodesender.gcode.util.GcodeUtils
import com.willwinder.universalgcodesender.model.UnitUtils
import java.io.PrintWriter
import java.util.Date

/*
  @author dastultz
  Created on Feb 14, 2019
*/

class SquareUpGenerator(var settings: SquareUpSettings) {

    fun unitMultiplier() = UnitUtils.scaleUnits(settings.units, UnitUtils.Units.MM)

    fun generate(output: PrintWriter) {
        // Set units and absolute movement/IJK mode.
        output.println("(Generated by Universal GCode Sender ${Date()})")
        output.println("${GcodeUtils.unitCommand(settings.units)} G90 G91.1")
        output.println("G17 F${settings.feed}")
        output.println("M3")

        //

        output.println("\n(All done!)")
        output.println("M5")
        output.println("M30")
    }
}
