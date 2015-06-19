package com.avpet.goblins

import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw.HTMLImageElement

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

object KeyEvent extends Enumeration {
  val DOM_VK_LEFT = Value(37)
  val DOM_VK_UP = Value(38)
  val DOM_VK_RIGHT = Value(39)
  val DOM_VK_DOWN = Value(40)
}

case class Point(x: Double, y: Double)

class ImageWithLoad extends HTMLImageElement {
  var onload: js.Function1[dom.Event, _] = ???
}

case class Hero(pos: Point,
                speed: Int = 256 // movement in pixels per second
                )

case class Monster(pos: Point)

@JSExport
object Goblins {
  @JSExport
  def main(canvas: html.Canvas): Unit = {
    /*setup*/
    val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

    canvas.width = 512
    canvas.height = 480

    // Background image
    var bgReady = false
    val bgImage = dom.document.createElement("img").asInstanceOf[ImageWithLoad]
    bgImage.onload = (e: dom.Event) => {
      bgReady = true
    }

    bgImage.src = "images/background.png"

    // Hero image
    var heroReady = false
    val heroImage = dom.document.createElement("img").asInstanceOf[ImageWithLoad]
    heroImage.onload = (e: dom.Event) => {
      heroReady = true
    }
    heroImage.src = "images/hero.png"

    // Monster image
    var monsterReady = false
    val monsterImage = dom.document.createElement("img").asInstanceOf[ImageWithLoad]
    monsterImage.onload = (e: dom.Event) => {
      monsterReady = true
    }
    monsterImage.src = "images/monster.png"

    // Game objects
    var hero = Hero(Point(0,0))
    var monster = Monster(Point(0,0))
    var monstersCaught = 0

    // Handle keyboard controls
    val keysDown = mutable.Map[Int, Boolean]()

    dom.addEventListener("keydown", (e: dom.KeyboardEvent) => {
      keysDown += e.keyCode ->true
    }, useCapture = false)

    dom.addEventListener("keyup", (e: dom.KeyboardEvent) => {
      keysDown -= e.keyCode
    }, useCapture = false)

    // Reset the game when the player catches a monster
    val reset = () => {
      hero = hero.copy(Point(x = canvas.width / 2, y = canvas.height / 2))

      // Throw the monster somewhere on the screen randomly
      monster = monster.copy(Point(x = 32 + (Math.random() * (canvas.width - 64)),
                                  y = 32 + (Math.random() * (canvas.height - 64))))
    }

    val render = () => {
      if (bgReady) {
        ctx.drawImage(bgImage, 0, 0)
      }

      if (heroReady) {
        ctx.drawImage(heroImage, hero.pos.x, hero.pos.y);
      }

      if (monsterReady) {
        ctx.drawImage(monsterImage, monster.pos.x, monster.pos.y);
      }

      // Score
      ctx.fillStyle = "rgb(250, 250, 250)";
      ctx.font = "24px Helvetica";
      ctx.textAlign = "left";
      ctx.textBaseline = "top";
      ctx.fillText("Goblins caught: " + monstersCaught, 32, 32);
    }

    // Update game objects
    val update = (modifier: Double) => {
      val delta = hero.speed * modifier

      def transformPoint(p: Point, cond: => Boolean, transfOp: => Point) =
        if (cond) transfOp else p

      val newPos = Seq(
        (p: Point) => {
          transformPoint(p, keysDown.contains(KeyEvent.DOM_VK_UP.id), p.copy(y = p.y - hero.speed * modifier))
        },
        (p: Point) => {
          transformPoint(p, keysDown.contains(KeyEvent.DOM_VK_DOWN.id), p.copy(y = p.y + hero.speed * modifier))
        },
        (p: Point) => {
          transformPoint(p, keysDown.contains(KeyEvent.DOM_VK_RIGHT.id), p.copy(x = p.x + hero.speed * modifier))
        },
        (p: Point) => {
          transformPoint(p, keysDown.contains(KeyEvent.DOM_VK_LEFT.id), p.copy(x = p.x - hero.speed * modifier))
      }).foldLeft(hero.pos) { (p, transfOp) => transfOp(p) }

      if (0 <= newPos.x && (newPos.x + 32) <= canvas.width && 0 <= newPos.y && (newPos.y + 32) <= canvas.height) {
        hero = hero.copy(pos = newPos)
      }

      // Are they touching?
      if (
        hero.pos.x <= (monster.pos.x + 32)
          && monster.pos.x <= (hero.pos.x + 32)
          && hero.pos.y <= (monster.pos.y + 32)
          && monster.pos.y <= (hero.pos.y + 32)
      ) {
        monstersCaught += 1
        reset()
      }

    }

    var then = js.Date.now()
    val loop = () => {
      val now = js.Date.now()
      val delta = now - then

      update(delta / 1000)
      render()

      then = now
    }

    reset()
    dom.setInterval(loop, 1)
  }

}
