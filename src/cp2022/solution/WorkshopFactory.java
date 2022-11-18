/*
 * University of Warsaw
 * Concurrent Programming Course 2022/2023
 * Java Assignment
 *
 * Author: Konrad Iwanicki (iwanicki@mimuw.edu.pl)
 */
package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.Workshop;

import java.util.Collection;


public final class WorkshopFactory {

  public static Workshop newWorkshop(Collection<Workplace> workplaces) {
    return new Werkstatt(workplaces);
  }

}
