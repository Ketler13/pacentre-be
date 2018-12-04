package com.howtographql.scala.sangria
import DBSchema._
import com.howtographql.scala.sangria.models.{AuthProviderSignupData, Suit, User, Fella}
import sangria.execution.deferred.{RelationIds, SimpleRelation}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Future

class DAO(db: Database) {
  def authenticate(email: String, password: String): Future[Option[User]] = db.run {
    Users.filter(u => u.email === email && u.password === password).result.headOption
  }

  def allSuits = db.run(Suits.result)

  def getSuits(ids: Seq[Int]): Future[Seq[Suit]] = db.run(
    Suits.filter(_.id inSet ids).result
  )

  def getSuitsByUsersIds(ids: Seq[Int]): Future[Seq[Suit]] = {
    db.run {
      Suits.filter(_.createdBy inSet ids).result
    }
  }

  def allUsers = db.run(Users.result)

  def getUsers(ids: Seq[Int]): Future[Seq[User]] = db.run(
    Users.filter(_.id inSet ids).result
  )

  def allFellas = db.run(Fellas.result)

  def getFellas(ids: Seq[Int]): Future[Seq[Fella]] = {
    db.run(
      Fellas.filter(_.id inSet ids).result
    )
  }

  def getFellasByRelationIds(rel: RelationIds[Fella]): Future[Seq[Fella]] =
    db.run(
      Fellas.filter { fella =>
        rel.rawIds.collect({
          case (SimpleRelation("byUser"), ids: Seq[Int]) => fella.addedBy inSet ids
          case (SimpleRelation("bySuit"), ids: Seq[Int]) => fella.suitId inSet ids
        }).foldLeft(true: Rep[Boolean])(_ || _)

      } result
    )

  def createUser(name: String, authProvider: AuthProviderSignupData): Future[User] = {
    val newUser = User(100, name, authProvider.email.email, authProvider.email.password)
    val insertAndReturnUserQuery = (Users returning Users.map(_.id)) into {
      (user, id) => user.copy(id = id)
    }
    db.run {
      insertAndReturnUserQuery += newUser
    }
  }

  def createSuit(title: String, description: String, addedBy: Int): Future[Suit] = {
    val insertAndReturnSuitQuery = (Suits returning Suits.map(_.id)) into {
      (suit, id) => suit.copy(id = id)
    }
    db.run {
      insertAndReturnSuitQuery += Suit(200, title, description, addedBy)
    }
  }

  def createFella(name: String, surname: String, nick: String, car: String, region: String, music: String, addedBy: Int, suitId: Int): Future[Fella] = {
    val insertAndReturnFellaQuery = (Fellas returning Fellas.map(_.id)) into {
      (fella, id) => fella.copy(id = id)
    }
    db.run {
      insertAndReturnFellaQuery += Fella(300, name, surname, nick, car, region, music, addedBy, suitId)
    }
  }
}