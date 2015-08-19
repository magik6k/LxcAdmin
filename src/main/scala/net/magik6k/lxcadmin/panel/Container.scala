package net.magik6k.lxcadmin.panel

import java.io.FileNotFoundException

import net.magik6k.jwwf.enums.Type
import net.magik6k.jwwf.widgets.basic.{TextLabel, ProgressBar}
import net.magik6k.jwwf.widgets.basic.panel.{NamedPanel, Row}
import net.magik6k.lxcadmin.util.{United, Using}
import net.magik6k.lxcadmin.widget.MemMonProvider

import scala.io.Source

class Container(val name: String) extends Row(4) {

	// CPU

	val cpuBars = Using(Source.fromFile("/sys/fs/cgroup/cpu/lxc/"+name+"/cpuacct.usage_percpu"))(_.mkString.split("\\s+").map(u => (new ProgressBar(0), u.toLong)))

	val barRow = new Row(cpuBars.length)
	cpuBars.foreach{case(bar, usage) => barRow.put(bar)}

	this.put(new NamedPanel(barRow, "CPU Usage", Type.SUCCESS))

	// MEMORY

	val memoryLimit = math.min(MemMonProvider.total * 1024, Using(Source.fromFile("/sys/fs/cgroup/memory/lxc/" + name + "/memory.limit_in_bytes"))(_.getLines().map(_.toLong).find(_=>true).get))
	var memoryUsed = Using(Source.fromFile("/sys/fs/cgroup/memory/lxc/" + name + "/memory.usage_in_bytes"))(_.getLines().map(_.toLong).next())
	val memoryBar = new ProgressBar(Type.SUCCESS, memoryUsed / memoryLimit.toDouble)
	val memoryLabel = new TextLabel("RAM (0%) 0M")
	this.put(new NamedPanel(new Row(2, memoryLabel.asPanel(3) , memoryBar.asPanel(9) ), "Memory", Type.WARNING))

	// NETWORK

	var lastExternalRX = try{Using(Source.fromFile("/sys/class/net/vethC" + name + "E/statistics/rx_bytes"))(_.getLines().map(_.toLong).next())}catch{case _: FileNotFoundException => 0}
	var lastExternalTX = try{Using(Source.fromFile("/sys/class/net/vethC" + name + "E/statistics/tx_bytes"))(_.getLines().map(_.toLong).next())}catch{case _: FileNotFoundException => 0}

	var lastInternalRX = try{Using(Source.fromFile("/sys/class/net/vethC" + name + "I/statistics/rx_bytes"))(_.getLines().map(_.toLong).next())}catch{case _: FileNotFoundException => 0}
	var lastInternalTX = try{Using(Source.fromFile("/sys/class/net/vethC" + name + "I/statistics/tx_bytes"))(_.getLines().map(_.toLong).next())}catch{case _: FileNotFoundException => 0}


	val externalNetworkUpload = new TextLabel("Upload: <b>0 Mib/s</b>")
	val externalNetworkDownload = new TextLabel("Download: <b>0 Mib/s</b>")

	val externalNetworkUploaded = new TextLabel("Uploaded: <b>" + lastExternalRX/1024/1024 + " MiB</b>")
	val externalNetworkDownloaded = new TextLabel("Downloaded: <b>" + lastExternalTX/1024/1024 + " MiB</b>")

	val internalNetworkUpload = new TextLabel("Upload: <b>0 Mib/s</b>")
	val internalNetworkDownload = new TextLabel("Download: <b>0 Mib/s</b>")

	val internalNetworkUploaded = new TextLabel("Uploaded: <b>" + lastInternalRX/1024/1024 + " MiB</b>")
	val internalNetworkDownloaded = new TextLabel("Downloaded: <b>" + lastInternalTX/1024/1024 + " MiB</b>")

	this.put(new NamedPanel(new Row(2,
		new NamedPanel(new Row(2,
			new Row(2, externalNetworkDownload.asPanel(12), externalNetworkUpload.asPanel(12)),
			new Row(2, externalNetworkDownloaded.asPanel(12), externalNetworkUploaded.asPanel(12))
		), "External", Type.DEFAULT).asPanel(12), new NamedPanel(new Row(2,
			new Row(2, internalNetworkDownload.asPanel(12), internalNetworkUpload.asPanel(12)),
			new Row(2, internalNetworkDownloaded.asPanel(12), internalNetworkUploaded.asPanel(12))
		), "Internal", Type.DEFAULT).asPanel(12)), "Network usage", Type.PRIMARY))

	// I/O




	// REFRESHING

	var lastTime = System.nanoTime()

	def refresh() {
		val time = System.nanoTime()

		//CPU

		Using(Source.fromFile("/sys/fs/cgroup/cpu/lxc/" + name + "/cpuacct.usage_percpu"))(_.mkString.split("\\s+"))
			.zipWithIndex.foreach{case (usage, id) =>
			val (bar, lastUse) = cpuBars(id)
			val use = usage.toLong
			bar.setProgress((use - lastUse) / (time - lastTime.toDouble))
			cpuBars(id) = (bar, use)
		}

		// MEMORY

		memoryUsed = Using(Source.fromFile("/sys/fs/cgroup/memory/lxc/" + name + "/memory.usage_in_bytes"))(_.getLines().map(_.toLong).next())
		memoryBar.setProgress(memoryUsed / memoryLimit.toDouble)
		memoryLabel.setText("RAM (" + math.floor((memoryUsed / memoryLimit.toDouble)*100).toInt + "%) " + (memoryUsed / 1024 / 1024) + "M")

		// NETWORK

		val eRX = try{Using(Source.fromFile("/sys/class/net/vethC" + name + "E/statistics/rx_bytes"))(_.getLines().map(_.toLong).next())}catch{case _: FileNotFoundException => 0}
		val eTX = try{Using(Source.fromFile("/sys/class/net/vethC" + name + "E/statistics/tx_bytes"))(_.getLines().map(_.toLong).next())}catch{case _: FileNotFoundException => 0}

		val iRX = try{Using(Source.fromFile("/sys/class/net/vethC" + name + "I/statistics/rx_bytes"))(_.getLines().map(_.toLong).next())}catch{case _: FileNotFoundException => 0}
		val iTX = try{Using(Source.fromFile("/sys/class/net/vethC" + name + "I/statistics/tx_bytes"))(_.getLines().map(_.toLong).next())}catch{case _: FileNotFoundException => 0}

		externalNetworkUpload.setText(s"Upload: <b>${math.floor((eRX - lastExternalRX) / 1024.0 / 1024.0 / ((time - lastTime.toDouble) / 1000000000.0) * 800) / 100.0} Mib/s</b>")
		externalNetworkDownload.setText(s"Download: <b>${math.floor((eTX - lastExternalTX) / 1024.0 / 1024.0 / ((time - lastTime.toDouble) / 1000000000.0) * 800) / 100.0} Mib/s</b>")
		externalNetworkUploaded.setText("Uploaded: <b>" + United(eRX, 1024, "iB") + "</b>")
		externalNetworkDownloaded.setText("Downloaded: <b>" + United(eTX, 1024, "iB") + "</b>")

		internalNetworkUpload.setText(s"Upload: <b>${math.floor((iRX - lastInternalRX) / 1024.0 / 1024.0 / ((time - lastTime.toDouble) / 1000000000.0) * 800) / 100.0} Mib/s</b>")
		internalNetworkDownload.setText(s"Download: <b>${math.floor((iTX - lastInternalTX) / 1024.0 / 1024.0 / ((time - lastTime.toDouble) / 1000000000.0) * 800) / 100.0} Mib/s</b>")
		internalNetworkUploaded.setText("Uploaded: <b>" + United(iRX, 1024, "iB") + "</b>")
		internalNetworkDownloaded.setText("Downloaded: <b>" + United(iTX, 1024, "iB") + "</b>")

		lastExternalRX = eRX
		lastExternalTX = eTX

		lastInternalRX = iRX
		lastInternalTX = iTX

		lastTime = time
	}
}
