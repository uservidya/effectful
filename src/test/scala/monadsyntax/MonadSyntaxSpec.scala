package monadsyntax

import scala.language.higherKinds

import MonadSyntax._

import scalaz._
import Scalaz._

import org.scalacheck.{Properties, Arbitrary}
import org.scalacheck.Prop._

object MonadSyntaxSpec extends Properties("MonadSyntax") {
  
  property("(monadically . extract) == id") = {
    def test[M[_], A](implicit 
      m: Monad[M], 
      a: Arbitrary[M[A]]) = forAll { (ma: M[A]) => 
        ma == monadically(extract(ma))
      }
    // TODO make a meta-QuickCheck that can generate types!
    test[List, Int] &&
    test[List, Boolean] &&
    test[Option, Int] &&
    test[Option, Boolean]
  }
  
  property("empty block with explicit type params") = {
    def test[M[_], A](implicit 
      m: Monad[M], 
      a: Arbitrary[A]) = forAll { (x: A) =>
        x.pure[M] == monadically[M, A](x)
      }
    
    test[List, Int] &&
    test[List, Boolean] &&
    test[Option, Int] &&
    test[Option, Boolean]
  }
  
  property("product") = {
    def test[M[_], A, B](implicit 
      m: Monad[M], 
      ama: Arbitrary[M[A]], 
      amb: Arbitrary[M[B]]) = forAll { (ma: M[A], mb: M[B]) =>
        
        val value = monadically {
          (extract(ma), extract(mb))
        }
        
        val expected = for {
          a <- ma
          b <- mb
        } yield (a, b)
        
        value == expected
      }
      
    test[List, Int, Char] &&
    test[Option, Boolean, Int]
  }
  

  
  property("nested blocks") = {
    def test[M[_], A, B](implicit 
      m: Monad[M], 
      ama: Arbitrary[M[A]], 
      amb: Arbitrary[M[B]]) = forAll { (ma: M[A], mb: M[B]) =>
        
        val value = monadically {
          val ma2 = monadically { (); extract(ma) }
          val mb2 = monadically { (); extract(mb) }
          (extract(ma2), extract(mb2))
        }
        
        val expected = for {
          a <- ma
          b <- mb
        } yield (a, b)

        
        value == expected
      }
      
    test[List, Int, Char] &&
    test[Option, Boolean, Int]
  }
  
  property("homogeneous conditional") = {
    def test[M[_], A, B](implicit 
      m: Monad[M], 
      at: Arbitrary[M[Boolean]], 
      al: Arbitrary[M[A]]) = forAll { (test: M[Boolean], left: M[A], right: M[A]) =>
        
        val value = monadically {
          if (extract(test)) 
            extract(left)
          else 
            extract(right)
        }
        
        val expected = for {
          t <- test
          result <- if (t) left else right
        } yield result
        
        value == expected
      }
      
    test[List, Int, Char] &&
    test[Option, Boolean, Int]
  }
  
  property("heterogeneous conditional") = {
    def test[M[_], A, B](implicit 
      m: Monad[M], 
      at: Arbitrary[M[Boolean]], 
      al: Arbitrary[M[A]],
      ar: Arbitrary[A]) = forAll { (test: M[Boolean], left: M[A], right: A) =>
        
        val value = monadically {
          if (extract(test)) 
            extract(left)
          else 
            right
        }
        
        val expected = for {
          t <- test
          result <- if (t) left else right.pure[M]
        } yield result
        
        value == expected
      }
      
    test[List, Int, Char] &&
    test[Option, Boolean, Int]
  }
  
  property("one-armed conditional") = {
    def test[M[_], A, B](implicit 
      m: Monad[M], 
      at: Arbitrary[M[Boolean]], 
      al: Arbitrary[M[A]],
      ar: Arbitrary[A]) = forAll { (test: M[Boolean], left: M[A], result: A) =>
        
        val value = monadically {
          if (extract(test)) 
            extract(left)
          result
        }
        
        val expected = for {
          t <- test
          _ <- if (t) left else ().pure[M]
        } yield result
        
        value == expected
      }
      
    test[List, Int, Char] &&
    test[Option, Boolean, Int]
  }
  
  property("heterogeneous else-if") = {
    def test[M[_], A, B](implicit 
      m: Monad[M], 
      at: Arbitrary[M[Boolean]], 
      aa: Arbitrary[A],
      ama: Arbitrary[M[A]]) = forAll { (test1: M[Boolean], choice1: M[A], test2: M[Boolean], choice2: A, choice3: M[A]) =>
        
        val value = monadically {
          if (extract(test1)) 
            extract(choice1)
          else if (extract(test2))
            choice2
          else extract(choice3)
        }
        
        val expected = for {
          t1 <- test1
          result <- if (t1) choice1 else {
            for {
              t2 <- test2
              innerResult <- if (t2) choice2.pure[M] else choice3
            } yield innerResult
          }
        } yield result
        
        value == expected
      }
      
    test[List, Int, Char] &&
    test[Option, Boolean, Int]
  }
}