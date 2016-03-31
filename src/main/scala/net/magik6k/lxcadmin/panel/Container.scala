package net.magik6k.lxcadmin.panel

import java.io.{File, FileNotFoundException}

import net.magik6k.jliblxc.LxcContainer
import net.magik6k.jwwf.enums.Type
import net.magik6k.jwwf.widgets.basic.{TextLabel, ProgressBar}
import net.magik6k.jwwf.widgets.basic.panel.{NamedPanel, Row}
import net.magik6k.lxcadmin.util.{United, Using}
import net.magik6k.lxcadmin.widget.{CPUMonProvider, MemMonProvider}

import scala.io.Source
import scala.util.matching.Regex

class Container(val name: String) extends Row(2) {
	private val container = new LxcContainer(name)

	val left = new Row(2)
	val right = new Row(2)

	val statusLabel = new TextLabel(if(container.isRunning) "Running" else "Not running")
	left.put(new NamedPanel(new Row(1, statusLabel), "Status", Type.SUCCESS).asPanel(12))

	//var cgbase = "/lxc/"
	//if(!new File("/sys/fs/cgroup/cpu/lxc/").exists()) {
	//	cgbase = "/init.scope/lxc/"
	//}

	//def cgroup(cg: String, name: String, group: String) = "/sys/fs/cgroup/" + cg + cgbase + name + "/" + group

	// CPU


	val cpuBars = (if(container.isRunning) container.getCgroupItem("cpuacct.usage_percpu") else "0 " * CPUMonProvider.cores).split("\\s+").map(u => (new ProgressBar(0), u.toLong))

	val barRow = new Row(cpuBars.length)
	cpuBars.foreach{case(bar, usage) => barRow.put(bar)}

	left.put(new NamedPanel(barRow, "CPU Usage", Type.SUCCESS).asPanel(12))

	// MEMORY

	val memoryLimit = math.min(MemMonProvider.total * 1024, if(container.isRunning) container.getCgroupItem("memory.limit_in_bytes").lines.map(_.toLong).find(_=>true).get else MemMonProvider.total * 1024)
	var memoryUsed = if(container.isRunning) container.getCgroupItem("memory.usage_in_bytes").lines.map(_.toLong).next() else 0
	val memoryBar = new ProgressBar(Type.SUCCESS, memoryUsed / memoryLimit.toDouble)
	val memoryLabel = new TextLabel("RAM (0%) 0M")
	right.put(new NamedPanel(new Row(2, memoryLabel.asPanel(3) , memoryBar.asPanel(9) ), "Memory", Type.WARNING).asPanel(12))


	// NETWORK
	case class Interface(name: String, var rx: Long, var tx: Long) {
		var lastRx: Long = 0
		var lastTx: Long = 0
		val pair = {
			var p: String = null
			var n = 0

			while(container.getRunningConfigItem(s"lxc.network.$n.type") != null) {
				if(container.getRunningConfigItem(s"lxc.network.$n.type") == "veth" && container.getRunningConfigItem(s"lxc.network.$n.name") == name) {
					p = container.getRunningConfigItem(s"lxc.network.$n.veth.pair")
				}
				n += 1
			}
			p
		}

		val ipv4: Seq[String] = container.getIps(name, "inet", 0)
		//val addresses = new Row(ipv4.length, ipv4.map(a => new TextLabel(a).asPanel(12)): _*)

		val upload = new TextLabel("Upload: <b>0 Mib/s</b>")
		val download = new TextLabel("Download: <b>0 Mib/s</b>")

		val uploaded = new TextLabel("Uploaded: <b>" + rx/1024/1024 + " MiB</b>")
		val downloaded = new TextLabel("Downloaded: <b>" + tx/1024/1024 + " MiB</b>")

		def forStat(stat: String, cb: (String)=>Unit) = try{Using(Source.fromFile("/sys/class/net/" + pair + "/statistics/" + stat))(b => cb(b.mkString))}catch{case _: FileNotFoundException => 0}
		def update(time: Long): Unit = {
			this.forStat("rx_bytes", n => rx = n.replaceAll("[^0-9]", "").toLong)
			this.forStat("tx_bytes", n => tx = n.replaceAll("[^0-9]", "").toLong)

			upload.setText(s"Upload: <b>${math.floor((rx - lastRx) / 1024.0 / 1024.0 / ((time - lastTime.toDouble) / 1000000000.0) * 800) / 100.0} Mib/s</b>")
			download.setText(s"Download: <b>${math.floor((tx - lastTx) / 1024.0 / 1024.0 / ((time - lastTime.toDouble) / 1000000000.0) * 800) / 100.0} Mib/s</b>")
			uploaded.setText("Uploaded: <b>" + United(rx, 1024, "iB") + "</b>")
			downloaded.setText("Downloaded: <b>" + United(tx, 1024, "iB") + "</b>")

			lastRx = rx
			lastTx = tx
		}
	}

	var interfaces: Seq[Interface] = null
	def genNetwork() {
		val ifrow = new Row(if (container.isRunning) container.getInterfaces.length else 0)
		val ifaces = (if (container.isRunning) container.getInterfaces else new Array[String](0)).map(i => Interface(i, 0, 0)).filter(_.pair != null)

		ifaces.foreach(_.update(0))
		ifaces.foreach(i => ifrow.put(new NamedPanel(new Row(2, //3,
			new Row(2, i.download.asPanel(12), i.upload.asPanel(12)),
			new Row(2, i.downloaded.asPanel(12), i.uploaded.asPanel(12))//,
			//i.addresses
		), i.name, Type.INFO).asPanel(12)))
		interfaces = ifaces
		if(container.isRunning)
			right.put(new NamedPanel(ifrow, "Network usage", Type.PRIMARY).asPanel(12), 1)
	}
	genNetwork()

	// I/O

	/*

	val blkioRegex = """[^\s]+\s+([^\s]+)""".r
	var lastUsedIoTime = {
		var s = 0
		Using(Source.fromFile(cgroup("blkio", name, "blkio.time_recursive"))){
			_.getLines().foreach{
				case blkioRegex(iotime) => s += iotime.toInt
				case _ => 0
			}
		}
		s
	}

	val ioTime = new TextLabel(s"IO Time: ${lastUsedIoTime/1000.0}")
	var ioUsage = new ProgressBar(Type.WARNING, 0)

	val blkioLongRegex = """(\d+:\d+) (\w+) (\d+)""".r
	var lastIoBytes = Map.empty[String, Long]

	Using(Source.fromFile(cgroup("blkio", name, "blkio.io_service_bytes_recursive"))){_.getLines().foreach {
		case blkioLongRegex(dev, key, value) => lastIoBytes += key -> value.toLong
		case _ =>
	}}

	val ioRead = new TextLabel(s"IO Read: <b>${United(lastIoBytes("Read"), 1024, "iB")}</b>(<b>0</b>B/s)")
	val ioWrite = new TextLabel(s"IO Write: <b>${United(lastIoBytes("Write"), 1024, "iB")}</b>(<b>0</b>B/s)")

	left.put(new NamedPanel(new Row(4, ioTime, ioUsage, ioRead, ioWrite), "IO", Type.DANGER).asPanel(12))

	//
	*/

	this.put(left)
	this.put(right)

	// REFRESHING

	var lastTime = System.nanoTime()
	var wasRunning = false

	def refresh() {
		val time = System.nanoTime()
		val running = container.isRunning
		statusLabel.setText(if(running) "Running" else "Not running")


		//CPU

		if(running) {
			container.getCgroupItem("cpuacct.usage_percpu").split("\\s+")
				.zipWithIndex.foreach { case (usage, id) =>
				val (bar, lastUse) = cpuBars(id)
				val use = usage.toLong
				bar.setProgress((use - lastUse) / (time - lastTime.toDouble))
				cpuBars(id) = (bar, use)
			}
		} else cpuBars.foreach(_._1.setProgress(0))



		// MEMORY

		val memoryLimit = math.min(MemMonProvider.total * 1024, if(container.isRunning) container.getCgroupItem("memory.limit_in_bytes").lines.map(_.toLong).find(_=>true).get else MemMonProvider.total * 1024)
		memoryUsed = if(container.isRunning) container.getCgroupItem("memory.usage_in_bytes").lines.map(_.toLong).next() else 0
		memoryBar.setProgress(memoryUsed / memoryLimit.toDouble)
		memoryLabel.setText("RAM (" + math.floor((memoryUsed / memoryLimit.toDouble)*100).toInt + "%) " + (memoryUsed / 1024 / 1024) + "M")


		// NETWORK

		if(running && !wasRunning) genNetwork()
		if(!running && wasRunning) {
			interfaces = Seq.empty
			right.put(null, 1)
		}
		interfaces.foreach(_.update(time))

		/*
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

		// IO

		val usedIoTime = {
			var s = 0
			Using(Source.fromFile(cgroup("blkio", name, "blkio.time_recursive"))){
				_.getLines().foreach{
					case blkioRegex(iotime) => s += iotime.toInt
					case _ => 0
				}
			}
			s
		}

		ioTime.setText(s"IO Time: ${usedIoTime/1000.0}s")
		ioUsage.setProgress((usedIoTime - lastUsedIoTime) / ((time - lastTime.toDouble) / 1000000))

		var ioBytes = Map.empty[String, Long]

		Using(Source.fromFile(cgroup("blkio", name, "blkio.io_service_bytes_recursive"))){_.getLines().foreach {
			case blkioLongRegex(dev, key, value) => ioBytes += key -> value.toLong
			case _ =>
		}}

		ioRead.setText(s"IO Read: <b>${United(lastIoBytes("Read"), 1024, "iB")}</b>(<b>${United(ioBytes("Read") - lastIoBytes("Read"), 1024, "iB")}</b>/s)")
		ioWrite.setText(s"IO Write: <b>${United(lastIoBytes("Write"), 1024, "iB")}</b>(<b>${United(ioBytes("Write") - lastIoBytes("Write"), 1024, "iB")}</b>/s)")

		lastUsedIoTime = usedIoTime
		lastIoBytes = ioBytes*/

		lastTime = time
		wasRunning = running
	}
}
