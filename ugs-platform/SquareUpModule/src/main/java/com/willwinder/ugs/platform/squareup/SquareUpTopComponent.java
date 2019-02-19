package com.willwinder.ugs.platform.squareup;

import com.willwinder.ugs.nbp.lib.services.LocalizingService;
import com.willwinder.ugs.nbp.lib.services.TopComponentLocalizer;
import com.willwinder.universalgcodesender.i18n.Localization;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.modules.OnStart;
import org.openide.windows.TopComponent;

import static com.willwinder.ugs.nbp.lib.services.LocalizingService.lang;

// can't figure out to do this strictly in Kotlin
// create a Java TopComponent and delegate to "panel"

@ConvertAsProperties(dtd = "-//com.willwinder.ugs.platform.squareup//SquareUp//EN", autostore = false)
@TopComponent.Description(preferredID = "SquareUpTopComponent",
		//iconBase="SET/PATH/TO/ICON/HERE",
		persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = SquareUpTopComponent.SquareUpCategory, id = SquareUpTopComponent.SquareUpActionId)
@ActionReference(path = LocalizingService.MENU_WINDOW_PLUGIN)
@TopComponent.OpenActionRegistration(displayName = "SquareUp", preferredID = "SquareUpTopComponent")
public final class SquareUpTopComponent extends TopComponent {

	private final static String SquareUpTitle = Localization.getString("platform.window.squareup-module", lang);
	private final static String SquareUpTooltip = Localization.getString("platform.window.squareup-module.tooltip", lang);
	final static String SquareUpActionId = "com.willwinder.ugs.platform.squareup.SquareUpTopComponent";
	final static String SquareUpCategory = LocalizingService.CATEGORY_WINDOW;

	private SquareUpPanel panel;

	@OnStart
	public static class Localizer extends TopComponentLocalizer {
		public Localizer() {
			super(SquareUpCategory, SquareUpActionId, SquareUpTitle);
		}
	}

	public SquareUpTopComponent() {
		setName(SquareUpTitle);
		setToolTipText(SquareUpTooltip);
		panel = new SquareUpPanel(this);
	}

	public SquareUpSettings getSettings() {
		return panel.getSettings();
	}

	@Override
	public void componentOpened() {
		panel.componentOpened();
	}

	@Override
	public void componentClosed() {
		panel.componentClosed();
	}

	public void writeProperties(java.util.Properties p) {
		panel.writeProperties(p);
	}

	public void readProperties(java.util.Properties p) {
		panel.readProperties(p);
	}
}