/*
    Copyright 2017 Will Winder

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

import com.jogamp.opengl.GL2
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.util.gl2.GLUT
import com.willwinder.ugs.nbm.visualizer.options.VisualizerOptions
import com.willwinder.ugs.nbm.visualizer.options.VisualizerOptions.VISUALIZER_OPTION_BOUNDRY_BASE
import com.willwinder.ugs.nbm.visualizer.shared.Renderable
import java.awt.Color
import javax.vecmath.Point3d

/*
  @author dastultz
  Created on Feb 14, 2019
*/

private const val CYLINDER_SLICES = 24
private const val CYLINDER_STACKS = 1

class SquareUpPreview(
    description: String,
    private val generator: SquareUpGenerator
) : Renderable(7, description) {

    companion object {
        val glut = GLUT()
    }

    // Preferences
    private lateinit var stockColor: Color

    override fun rotate() = true
    override fun center() = true

    override fun init(drawable: GLAutoDrawable) {
        reloadPreferences(VisualizerOptions())
    }

    override fun reloadPreferences(vo: VisualizerOptions) {
        // todo: this isn't working
        stockColor = vo.getOptionForKey(VISUALIZER_OPTION_BOUNDRY_BASE)!!.value
    }

    override fun draw(
        drawable: GLAutoDrawable,
        idle: Boolean,
        machineCoord: Point3d,
        workCoord: Point3d,
        objectMin: Point3d,
        objectMax: Point3d,
        scaleFactor: Double,
        mouseWorldCoordinates: Point3d,
        rotation: Point3d
    ) {
        if (drawable.gl.gL2 == null) return

        if (generator.settings.operation.isMilling) {
            drawMillingPreview(drawable)
        } else {
            drawTurningPreview(drawable)
        }
    }

    private fun drawTurningPreview(drawable: GLAutoDrawable) {
        val convert = generator::unitConverter
        val settings = generator.settings
        val diameter = convert(settings.stockDiameter / 2.0)
        val remainingStockLength = convert(settings.stockLength - settings.totalStepDown)
        val removedStockLength = convert(settings.totalStepDown)

        val gl = drawable.gl.gL2
        gl.glLineWidth(0.5f)
        val color = Color(200, 200, 200, 200)
        gl.glColor4fv(VisualizerOptions.colorToFloatArray(color), 0)

        gl.glPushMatrix()
        gl.glTranslated(0.0, 0.0, 0 - removedStockLength)
        glut.glutWireCylinder(diameter, removedStockLength, CYLINDER_SLICES, CYLINDER_STACKS)
        gl.glTranslated(0.0, 0.0, 0 - remainingStockLength)
        glut.glutSolidCylinder(diameter, remainingStockLength, CYLINDER_SLICES, CYLINDER_STACKS)
        gl.glPopMatrix()
    }

    private fun drawMillingPreview(drawable: GLAutoDrawable) {
        val convert = generator::unitConverter
        val width = (convert(generator.settings.stockWidth)).toFloat()
        val mid = (convert(0 - generator.settings.totalStepDown).toFloat())
        val height = (convert(0 - generator.settings.stockHeight).toFloat())
        val depth = (convert(generator.settings.stockDepth).toFloat())

        val gl = drawable.gl.gL2

        gl.glPushMatrix()
        gl.glTranslated(0.001, 0.001, 0.001)

        val color = Color(200, 200, 200, 200)
        gl.glColor4fv(VisualizerOptions.colorToFloatArray(color), 0)

        val upper_left_near = floatArrayOf(0f, 0f, 0f)
        val upper_right_near = floatArrayOf(width, 0f, 0f)
        val upper_left_far = floatArrayOf(0f, depth, 0f)
        val upper_right_far = floatArrayOf(width, depth, 0f)
        val mid_left_near = floatArrayOf(0f, 0f, mid)
        val mid_right_near = floatArrayOf(width, 0f, mid)
        val mid_left_far = floatArrayOf(0f, depth, mid)
        val mid_right_far = floatArrayOf(width, depth, mid)
        val lower_left_near = floatArrayOf(0f, 0f, height)
        val lower_right_near = floatArrayOf(width, 0f, height)
        val lower_left_far = floatArrayOf(0f, depth, height)
        val lower_right_far = floatArrayOf(width, depth, height)

        drawSolidBox(
            gl,
            mid_left_near,
            mid_right_near,
            mid_right_far,
            mid_left_far,
            lower_left_near,
            lower_right_near,
            lower_right_far,
            lower_left_far
        )

        drawOutlineBox(
            gl,
            upper_left_near,
            upper_right_near,
            upper_right_far,
            upper_left_far,
            mid_left_near,
            mid_right_near,
            mid_right_far,
            mid_left_far
        )

        gl.glFlush()
        gl.glPopMatrix()
    }

    private fun drawOutlineBox(
        gl: GL2,
        upper_left_near: FloatArray,
        upper_right_near: FloatArray,
        upper_right_far: FloatArray,
        upper_left_far: FloatArray,
        lower_left_near: FloatArray,
        lower_right_near: FloatArray,
        lower_right_far: FloatArray,
        lower_left_far: FloatArray
    ) {
        gl.glLineWidth(0.5f)

        gl.glBegin(GL2.GL_LINE_STRIP)

        gl.glVertex3fv(upper_left_near, 0)
        gl.glVertex3fv(upper_right_near, 0)
        gl.glVertex3fv(upper_right_far, 0)
        gl.glVertex3fv(upper_left_far, 0)
        gl.glVertex3fv(upper_left_near, 0)
        gl.glVertex3fv(lower_left_near, 0)
        gl.glVertex3fv(lower_right_near, 0)
        gl.glVertex3fv(lower_right_far, 0)
        gl.glVertex3fv(lower_left_far, 0)
        gl.glVertex3fv(lower_left_near, 0)

        gl.glEnd()
        gl.glBegin(GL2.GL_LINES)

        gl.glVertex3fv(lower_right_near, 0)
        gl.glVertex3fv(upper_right_near, 0)
        gl.glVertex3fv(lower_right_far, 0)
        gl.glVertex3fv(upper_right_far, 0)
        gl.glVertex3fv(lower_left_far, 0)
        gl.glVertex3fv(upper_left_far, 0)

        gl.glEnd()
    }

    private fun drawSolidBox(
        gl: GL2,
        upper_left_near: FloatArray,
        upper_right_near: FloatArray,
        upper_right_far: FloatArray,
        upper_left_far: FloatArray,
        lower_left_near: FloatArray,
        lower_right_near: FloatArray,
        lower_right_far: FloatArray,
        lower_left_far: FloatArray
    ) {
        gl.glBegin(GL2.GL_QUADS)
        // Top
        gl.glNormal3f(0f, 0f, 1.0f)
        gl.glVertex3fv(upper_left_near, 0)
        gl.glVertex3fv(upper_right_near, 0)
        gl.glVertex3fv(upper_right_far, 0)
        gl.glVertex3fv(upper_left_far, 0)

        // Bottom
        gl.glNormal3f(0f, 0f, -1.0f)
        gl.glVertex3fv(lower_left_near, 0)
        gl.glVertex3fv(lower_right_near, 0)
        gl.glVertex3fv(lower_right_far, 0)
        gl.glVertex3fv(lower_left_far, 0)

        // Front
        gl.glNormal3f(0f, -1.0f, 0f)
        gl.glVertex3fv(upper_left_near, 0)
        gl.glVertex3fv(upper_right_near, 0)
        gl.glVertex3fv(lower_right_near, 0)
        gl.glVertex3fv(lower_left_near, 0)

        // Back
        gl.glNormal3f(0f, 1.0f, 0f)
        gl.glVertex3fv(upper_left_far, 0)
        gl.glVertex3fv(upper_right_far, 0)
        gl.glVertex3fv(lower_right_far, 0)
        gl.glVertex3fv(lower_left_far, 0)

        // Left
        gl.glNormal3f(-1.0f, 0f, 0f)
        gl.glVertex3fv(upper_left_far, 0)
        gl.glVertex3fv(upper_left_near, 0)
        gl.glVertex3fv(lower_left_near, 0)
        gl.glVertex3fv(lower_left_far, 0)

        // Right
        gl.glNormal3f(1.0f, 0f, 0f)
        gl.glVertex3fv(upper_right_near, 0)
        gl.glVertex3fv(upper_right_far, 0)
        gl.glVertex3fv(lower_right_far, 0)
        gl.glVertex3fv(lower_right_near, 0)

        gl.glEnd()
    }
}
