package net.magik6k.lxcadmin.panel

import net.magik6k.jliblxc.Lxc
import net.magik6k.jwwf.enums.Type
import net.magik6k.jwwf.util.Tab
import net.magik6k.jwwf.widgets.basic.panel.{TabbedPanel, NamedPanel, Panel, Row}

class LxcPanel extends Panel(12, 1) {

	val filelist = Lxc.getContainers

	filelist.foreach(println(_))
	val systems = filelist
	var row = new Row(2)

	val containerList = systems.map(file => new Container(file))

	var containers = new TabbedPanel(systems.length,
		containerList.map(container => new Tab(new NamedPanel(container, "Container <b>"+container.name+"</b>", Type.INFO), container.name, Type.SUCCESS)).toArray:_*)

	row.put(containers.asPanel(12))

	put(row)

	def refresh() {
		containerList.foreach(_.refresh())
	}
}
