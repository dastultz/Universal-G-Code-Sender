/*
    Copyright 2017-2018 Will Winder

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

import com.google.gson.Gson
import com.willwinder.ugs.nbm.visualizer.shared.RenderableUtils
import com.willwinder.ugs.nbp.lib.lookup.CentralLookup
import com.willwinder.universalgcodesender.i18n.Localization
import com.willwinder.universalgcodesender.model.BackendAPI
import com.willwinder.universalgcodesender.model.UnitUtils
import com.willwinder.universalgcodesender.uielements.helpers.ThemeColors
import com.willwinder.universalgcodesender.utils.GUIHelpers
import com.willwinder.universalgcodesender.utils.SwingHelpers
import com.willwinder.universalgcodesender.utils.SwingHelpers.getDouble
import com.willwinder.universalgcodesender.utils.SwingHelpers.selectedUnit
import com.willwinder.universalgcodesender.utils.SwingHelpers.unitIdx
import net.miginfocom.swing.MigLayout
import java.awt.BorderLayout
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.Files
import java.util.Properties
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.ScrollPaneConstants
import javax.swing.SpinnerNumberModel

/*
  @author dastultz
  Created on Feb 14, 2019
*/

private const val i18nBase = "platform.plugin.squareup-module"

enum class Operation(private val i18nKey: String, val milling: Boolean) {
    MILL_FACING("milling-face", true),
    TURN_FACING("turning-face", false);

    override fun toString(): String {
        return Localization.getString("$i18nBase.$i18nKey")
    }

    val isMilling: Boolean
        get() = milling
}

class SquareUpPanel(val topComponent: SquareUpTopComponent) {

    companion object {
        private const val JSON_PROPERTY = "squareup_settings_json"
        private const val ERROR_GENERATING = "An error occurred generating SquareUp program: "
        private const val ERROR_LOADING = "An error occurred loading generated SquareUp program: "

        private const val doubleSpinnerMax = 1000000.0

        private val GSON = Gson()
    }

    private val frameBorder = BorderFactory.createLineBorder(ThemeColors.GREY)
    private val scrollablePanel = JPanel(MigLayout("fillx, wrap 1, inset 0", "grow"))

    // Inputs
    private val operation = JComboBox<Operation>(Operation.values())
    private val stockWidth = SpinnerNumberModel(5.0, 0.0, doubleSpinnerMax, 0.1)
    private val stockDepth = SpinnerNumberModel(5.0, 0.0, doubleSpinnerMax, 0.1)
    private val stockHeight = SpinnerNumberModel(10.0, 0.0, doubleSpinnerMax, 0.1)
    private val stockDiameter = SpinnerNumberModel(5.0, 0.0, doubleSpinnerMax, 0.1)
    private val stockLength = SpinnerNumberModel(10.0, 0.0, doubleSpinnerMax, 0.1)
    private val bitDiameter = SpinnerNumberModel(3.17, 0.0, doubleSpinnerMax, 0.1)
    private val stepOver = SpinnerNumberModel(1.0, 0.0, doubleSpinnerMax, 0.1)
    private val maxStepDown = SpinnerNumberModel(3.0, 0.0, doubleSpinnerMax, 0.1)
    private val totalStepDown = SpinnerNumberModel(10.0, 0.0, doubleSpinnerMax, 0.1)
    private val crossCenter = SpinnerNumberModel(0.0, 0.0, doubleSpinnerMax, 0.1)
    private val cuttingFeedRate = SpinnerNumberModel(100.0, 1.0, doubleSpinnerMax, 1.0)
    private val safetyHeight = SpinnerNumberModel(5.0, 0.0, doubleSpinnerMax, 0.1)
    private val units = JComboBox(SwingHelpers.getUnitOptions())

    private val fineDimensionSpinners = listOf(
        stockWidth, stockDepth, stockHeight,
        stockDiameter, stockLength,
        safetyHeight, bitDiameter,
        stepOver, maxStepDown, totalStepDown,
        crossCenter
    )

    private val coarseDimensionSpinners = listOf(
        cuttingFeedRate
    )

    // Buttons
    private val generateGCodeButton = JButton(Localization.getString("$i18nBase.generate"))
    private val exportGCodeButton = JButton(Localization.getString("$i18nBase.export"))

    private val generator = SquareUpGenerator(settings)
    private val preview = SquareUpPreview(Localization.getString("$i18nBase.preview"), generator)

    private val backend = CentralLookup.getDefault().lookup(BackendAPI::class.java)

    private val currentOperation: Operation
        get() = operation.selectedItem as Operation

    val settings: SquareUpSettings
        get() = SquareUpSettings(
            currentOperation,
            getDouble(this.stockWidth),
            getDouble(this.stockDepth),
            getDouble(this.stockHeight),
            getDouble(this.stockDiameter),
            getDouble(this.stockLength),
            getDouble(this.bitDiameter),
            getDouble(this.stepOver),
            getDouble(this.maxStepDown),
            getDouble(this.totalStepDown),
            getDouble(this.crossCenter),
            getDouble(this.cuttingFeedRate),
            getDouble(this.safetyHeight),
            selectedUnit(this.units.selectedIndex)
        )

    init {
        setupGui()
    }

    private fun setupGui() {
        // Button callbacks
        generateGCodeButton.addActionListener { _ -> onGenerateGCode() }
        exportGCodeButton.addActionListener { _ -> onExportGCode() }

        // Change listeners
        (fineDimensionSpinners + coarseDimensionSpinners).forEach {
            it.addChangeListener { _ -> onSpinnerChanged() }
        }

        units.addActionListener { _ -> onUnitChanged() }
        operation.addActionListener { _ -> onOperationChanged() }

        // Main layout
        topComponent.layout = BorderLayout()

        val fixedTopPanel = JPanel(MigLayout("fillx, wrap 2, inset 2", "grow"))
        fixedTopPanel.border = BorderFactory.createTitledBorder(
            frameBorder, Localization.getString("$i18nBase.global")
        )
        topComponent.add(fixedTopPanel, BorderLayout.NORTH)

        fixedTopPanel.add(JLabel(Localization.getString("$i18nBase.operation")), "growx")
        fixedTopPanel.add(operation, "growx")
        fixedTopPanel.add(JLabel(Localization.getString("gcode.setting.units")), "growx")
        fixedTopPanel.add(units, "growx")

        val scrollPane = JScrollPane(scrollablePanel)
        scrollPane.border = BorderFactory.createEmptyBorder()
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        topComponent.add(scrollPane, BorderLayout.CENTER)

        topComponent.add(scrollPane, BorderLayout.CENTER)

        val fixedBottomPanel = JPanel(MigLayout("fillx, wrap 1, inset 0", "grow"))
        topComponent.add(fixedBottomPanel, BorderLayout.SOUTH)
        fixedBottomPanel.add(generateGCodeButton, "growx")
        fixedBottomPanel.add(exportGCodeButton, "growx")

        buildSubPanels()
        applyUnitsToSpinners()
    }

    private fun buildSubPanels() {

        /*
        Tool tab includes cutter diameter, feed, speed, lead in/out rate
        Geometry is that to be cut
        Heights for clearance, retract, top, bottom
        Passes for step over, climb/conventional


        --
        op type
            face milling
            side milling
            face turning
            profile turning
        units
            mm
            in

        -- stock
        (milling)
            stock width
            stock depth
            stock height
        (turning)
            stock width "diameter"
            stock height "length"

        -- cutting parameters
        cutting feedrate
        lead-in feedrate
        milling
            √tool diameter
            √safety height
            type climb/conventional
            face milling
                √overlap "stepover"
                √max cut "max stepdown"
                √total cut "total stepdown"
            side milling
                orientation: left, right, front, back
                overlap "max depth of cut"
                max cut "max stepover"
                total cut "total stepover"
        turning
            face turning
                max cut "max stepdown"
                total cut "total stepdown"
                cross center
            profile turning
                max cut "max stepover"
                total cut "total stepover"
         */

        scrollablePanel.removeAll()
        val stockPanel = JPanel(MigLayout("fillx, wrap 2, inset 2"))
        val operationPanel = JPanel(MigLayout("fillx, wrap 2, inset 2"))
        scrollablePanel.add(stockPanel, "growx")
        scrollablePanel.add(operationPanel, "growx")

        stockPanel.border = BorderFactory.createTitledBorder(
            frameBorder,
            Localization.getString("$i18nBase.stock")
        )
        operationPanel.border = BorderFactory.createTitledBorder(
            frameBorder,
            Localization.getString("$i18nBase.operation-params")
        )

        val isMilling = currentOperation.isMilling
        stockPanel.removeAll()
        if (isMilling) {
            stockPanel.appendSpinner("stock-width", stockWidth)
            stockPanel.appendSpinner("stock-depth", stockDepth)
            stockPanel.appendSpinner("stock-height", stockHeight)
        } else {
            stockPanel.appendSpinner("stock-diameter", stockDiameter)
            stockPanel.appendSpinner("stock-length", stockLength)
        }

        operationPanel.removeAll()
        if (isMilling) {
            operationPanel.appendSpinner("gcode.setting.endmill-diameter", bitDiameter)
            operationPanel.appendSpinner("stepover", stepOver)
            operationPanel.appendSpinner("maxstepdown", maxStepDown)
            operationPanel.appendSpinner("totalstepdown", totalStepDown)
        } else {
            operationPanel.appendSpinner("maxstepdown", maxStepDown)
            operationPanel.appendSpinner("totalstepdown", totalStepDown)
            operationPanel.appendSpinner("crosscenter", crossCenter)
        }
        operationPanel.appendSpinner("gcode.setting.feed", cuttingFeedRate)
        operationPanel.appendSpinner("gcode.setting.safety-height", safetyHeight)

        topComponent.revalidate()
        topComponent.repaint()
    }

    private fun JComponent.appendSpinner(i18nKey: String, model: SpinnerNumberModel) {
        val fullI18NKey = if (i18nKey.contains(".")) i18nKey else "$i18nBase.$i18nKey"
        add(JLabel(Localization.getString(fullI18NKey)), "growx")
        add(JSpinner(model), "growx")
    }

    private fun generateAndLoadGCode(file: File) {
        try {
            PrintWriter(FileWriter(file)).use { writer -> generator.generate(writer) }
            backend.gcodeFile = file
        } catch (exc: IOException) {
            GUIHelpers.displayErrorDialog("$ERROR_GENERATING ${exc.localizedMessage}")
        } catch (exc: Exception) {
            GUIHelpers.displayErrorDialog("$ERROR_LOADING ${exc.localizedMessage}")
        }
    }

    private fun onGenerateGCode() {
        try {
            val path = Files.createTempFile("squareup_program", ".gcode")
            val file = path.toFile()
            generateAndLoadGCode(file)
        } catch (exc: IOException) {
            GUIHelpers.displayErrorDialog("$ERROR_LOADING ${exc.localizedMessage}")
        }
    }

    private fun onExportGCode() {
        val sourceDir = backend.settings.lastOpenedFilename
        SwingHelpers
            .createFile(sourceDir)
            .ifPresent { file -> generateAndLoadGCode(file) }
    }

    private fun onOperationChanged() {
        buildSubPanels()
        updateGenerator()
    }

    private fun onUnitChanged() {
        applyUnitsToSpinners()
        updateGenerator()
    }

    private fun onSpinnerChanged() = updateGenerator()

    private fun updateGenerator() {
        this.generator.settings = settings
    }

    private fun applyUnitsToSpinners() {
        val fine = if (settings.units == UnitUtils.Units.INCH) 0.001 else 0.1
        fineDimensionSpinners.forEach { it.stepSize = fine }
        val coarse = if (settings.units == UnitUtils.Units.INCH) 0.5 else 10.0
        coarseDimensionSpinners.forEach { it.stepSize = coarse }
    }

    fun componentOpened() {
        RenderableUtils.registerRenderable(preview)
    }

    fun componentClosed() {
        RenderableUtils.removeRenderable(preview)
    }

    fun writeProperties(properties: Properties) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        properties.setProperty("version", "1.0")
        properties.setProperty(JSON_PROPERTY, GSON.toJson(settings))
    }

    fun readProperties(properties: Properties) {
        if (properties.containsKey(JSON_PROPERTY)) {
            val json = properties.getProperty(JSON_PROPERTY)
            try {
                val settings = Gson().fromJson(json, SquareUpSettings::class.java)
                this.operation.selectedItem = settings.operation
                this.stockWidth.value = settings.stockWidth
                this.stockDepth.value = settings.stockDepth
                this.stockHeight.value = settings.stockHeight
                this.stockDiameter.value = settings.stockDiameter
                this.stockLength.value = settings.stockLength
                this.bitDiameter.value = settings.bitDiameter
                this.stepOver.value = settings.stepOver
                this.maxStepDown.value = settings.maxStepDown
                this.crossCenter.value = settings.crossCenter
                this.totalStepDown.value = settings.totalStepDown
                this.cuttingFeedRate.value = settings.feed
                this.safetyHeight.value = settings.safetyHeight
                this.units.selectedIndex = unitIdx(settings.units)
            } catch (exc: Exception) {
                GUIHelpers.displayErrorDialog("Problem loading SquareUp Settings, defaults have been restored.")
            }
        }
    }
}