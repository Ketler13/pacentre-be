package com.howtographql.scala.sangria

import java.sql.Timestamp

import akka.http.scaladsl.model.DateTime
import com.howtographql.scala.sangria.models._
import slick.jdbc.H2Profile.api._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps


object DBSchema {

  implicit val dateTimeColumnType = MappedColumnType.base[DateTime, Timestamp](
    dt => new Timestamp(dt.clicks),
    ts => DateTime(ts.getTime)
  )


  class SuitsTable(tag: Tag) extends Table[Suit](tag, "SUITS"){

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def title = column[String]("TITLE")
    def description = column[String]("DESCRIPTION")
    def createdBy = column[Int]("ADDED_BY")
    def createdAt = column[DateTime]("CREATED_AT")

    def * = (id, title, description, createdBy, createdAt).mapTo[Suit]

    def createdByFK = foreignKey("createdBy_FK", createdBy, Users)(_.id)

  }

  val Suits = TableQuery[SuitsTable]

  class UsersTable(tag: Tag) extends Table[User](tag, "USERS"){
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def email = column[String]("EMAIL")
    def password = column[String]("PASSWORD")
    def createdAt = column[DateTime]("CREATED_AT")

    def * = (id, name, email, password, createdAt).mapTo[User]
  }

  val Users = TableQuery[UsersTable]

  class FellasTable(tag: Tag) extends Table[Fella](tag, "FELLAS"){
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def surname = column[String]("SURNAME")
    def nick = column[String]("NICK")
    def car = column[String]("CAR")
    def region = column[String]("REGION")
    def music = column[String]("MUSIC")
    def addedBy = column[Int]("ADDED_BY")
    def suitId = column[Int]("SUIT_ID")
    def createdAt = column[DateTime]("CREATED_AT")

    def * = (id, name, surname, nick, car, region, music, addedBy, suitId, createdAt).mapTo[Fella]

    def addedByFK = foreignKey("addedBy_FK", addedBy, Users)(_.id)
    def suitIdFK = foreignKey("suitId_FK", suitId, Suits)(_.id)
  }

  val Fellas = TableQuery[FellasTable]

  /**
    * Load schema and populate sample data within this Sequence od DBActions
    */
  val databaseSetup = DBIO.seq(
    Users.schema.create,
    Suits.schema.create,
    Fellas.schema.create,

    Users forceInsertAll Seq(
      User(100, "Сержант Петренко", "petrenko@serg.pol.us", "secret", DateTime(1985,9,12)),
      User(101, "Лейтенант Грязнов", "tanki@vboy.pol.us", "123456", DateTime(1988,3,10)),
      User(102, "Полковник Дрипало", "greenpeace@gmail.com", "424242", DateTime(1972,2,8)),
      User(103, "Сержант Гринько", "grinko@yandex.ru", "777777", DateTime(1984,12,2)),
      User(104, "Лейтенант Думенко", "dumenko@leit.coom", "777777", DateTime(1975,12,2)),
      User(105, "Майор Рожин", "rozhin@yandex.ru", "777777", DateTime(1975,12,2)),
      User(106, "Подполковник Чушков", "chushkov-col@yandex.ru", "654321", DateTime(1965,9,2)),
    ),

    Suits forceInsertAll Seq(
      Suit(200, "Баклан", "Хулиган", 100, DateTime(2017,9,12)),
      Suit(201, "Барыга", "Наркоторговец", 100, DateTime(2017,3,11)),
      Suit(202, "Голубятник", "Вор, совершающий кражи с веревок", 100, DateTime(2016,6,7)),
      Suit(203, "Медвежатник", "Взломщик сейфов", 100, DateTime(2018,4,4)),
      Suit(204, "Князь", "Вор-рецидивист", 100, DateTime(2014,9,7)),
      Suit(205, "Кондуктор", "Босяк", 100, DateTime(2017,1,12)),
      Suit(206, "Лунатик", "Грабитель-одиночка", 100, DateTime(2016,6,24)),
      Suit(207, "Маз", "Главарь шайки", 100, DateTime(2018,1,17)),
      Suit(208, "Очкарь", "Вор-форточник", 100, DateTime(2014,7,21)),
      Suit(209, "Пахан", "Вор-авторитет", 100, DateTime(2017,7,8)),
      Suit(210, "Щипач", "Вор-карманник", 100, DateTime(2017,9,12)),
    ),

    Fellas forceInsertAll Seq(
      Fella(300, "Дмитрий", "Голубчиков", "Грязный", "Ваз 2108", "Арки", "Бутырка", 100, 200, DateTime(1983,3,10)),
      Fella(301, "Борис", "Куценко", "Цапля", "ВMW 318", "АТБ", "Трофим", 100, 201, DateTime(1981,2,2)),
      Fella(302, "Григорий", "Богомолов", "Ворона", "Пешком", "Арки", "Бутырка", 101, 202, DateTime(1994,3,10)),
      Fella(303, "Роман", "Овчаренко", "Король", "Пешком", "Падик", "AK47", 101, 203, DateTime(1992,10,8)),
      Fella(304, "Игорь", "Воротниченко", "Сиплый", "Daewoo Lanos", "Арки", "Rammstein", 102, 204, DateTime(1983,6,6)),
      Fella(305, "Степан", "Жмурко", "Жмур", "Ваз 2106", "Парк", "Шуфутинский", 102, 205, DateTime(1972,5,12)),
      Fella(306, "Иван", "Приступа", "Шнырь", "Mercedes C220", "Детская площадка", "GuF", 100, 206, DateTime(1976,10,3)),
      Fella(307, "Антон", "Смельчаков", "Смелый", "Ваз 2103", "АТБ", "Лепс", 103, 207, DateTime(1991,11,6)),
      Fella(308, "Сергей", "Мутко", "Мутный", "Renaul Laguna", "Varus", "Тимати", 103, 208, DateTime(1983,5,8)),
      Fella(309, "Игорь", "Сырко", "Мыша", "Ваз 2104", "АТБ", "Клубнячок", 100, 209, DateTime(1980,1,1)),
      Fella(310, "Дмитрий", "Рожницкий", "Гнида", "BMW 740", "Кафе Восток", "GuF", 101, 210, DateTime(1990,8,2)),
    )
  )



  def createDatabase: DAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new DAO(db)

  }

}