import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import at.searles.graphdrawing.Leveling;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class TestDrawing extends Application {
	
	Map<String, Set<String>> graph = new TreeMap<String, Set<String>>();
	
	void connect(String src, String dst) {
		if(!graph.containsKey(src)) {
			graph.put(src, new TreeSet<String>());
		}
		
		graph.get(src).add(dst);
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	void graph1() {
		connect("fc", "fe");
		connect("fc", "c");
		connect("c", "e");
		connect("fe", "e");
		connect("fc", "fk");
		connect("fd", "fk");
		connect("c", "k");
		connect("d", "k");
	}
	
	void graph2() {
		// Complex loop
		// Example why unr*unr does not work
		connect("1", "7");
		connect("1", "8");
		connect("1", "9");
		connect("9", "1");
		connect("9", "0");
		connect("9", "4");
		connect("4", "8");
		connect("4", "3");
		connect("3", "4");
	}
	
	void graph3() {
		// it is 2 -> 5, 2 -> 6 -> 7 -> 2, 6 -> 4, 6 -> 8 -> 2	
		connect("0", "0");
		connect("2", "2");
		connect("2", "3");
		connect("3", "0");
	}
	
	void graph4() {
		//connect("0", "8");
		//connect("8", "8");
		connect("8", "9");
		connect("3", "1");
		connect("3", "2");
		connect("3", "9");
		connect("4", "3");
		//connect("4", "5");
		//connect("4", "7");
		//connect("5", "7");		
	}
	
	void rndGraph(int maxNodes, int edgeCount) {
		Random rnd = new Random();
		
		for(int i = 0; i < edgeCount; i++) {
			connect(Integer.toString(Math.abs(rnd.nextInt()) % maxNodes), Integer.toString(Math.abs(rnd.nextInt()) % maxNodes));
		}
	}

	@Override
	public void start(Stage stage) {
		Button button = new Button("Next");
		Group root = new Group();
		BorderPane pane = new BorderPane();

		Leveling<String> leveling = new Leveling<String>();

		button.setOnAction((ActionEvent event)-> {
			root.getChildren().clear();
			
			Leveling<String> l;
			
			int i = 0;
			int j = 12;
			
			//do {
				graph.clear();
				rndGraph(j, j - 2);
				leveling.setGraph(graph);
				
				if(i > j*j) {
					i = 0;
					j++;
					System.out.println("checking " + j);
				} else {
					i++;
				}
			//} while(l.checkGraphIntegrity());
			
			leveling.draw(root);
			pane.setCenter(root);
		});

		graph2();
		//rndGraph(10, 10);
		leveling.setGraph(graph);
		leveling.draw(root);
		
		pane.setCenter(root);
		pane.setBottom(button);
		
        stage.setScene(new Scene(pane));
        stage.show();
	}
}
