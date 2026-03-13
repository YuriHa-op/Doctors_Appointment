package view;


  //Abstraction so Views can trigger navigation (e.g. logout)
  //without depending on the concrete MainFrame class

public interface NavigationHandler {
    void logout();
}
