var java = require("java")

var JFrame = java.import("javax.swing.JFrame");
var JTextField = java.import("javax.swing.JTextField");
var JLabel = java.import("javax.swing.JLabel");
var JButton = java.import("javax.swing.JButton");
var GridLayout = java.import("java.awt.GridLayout");

var frame = new JFrame("Celsius Converter")
var temp_text = new JTextField()
var celsius_label = new JLabel("Celsius")
var convert_button = new JButton("Convert")
var fahrenheit_label = new JLabel("Fahrenheit")

convert_button.addActionListener(new java.Proxy("java.awt.event.ActionListener",
	{
		actionPerformed: function (evt) {
			var c = Number(temp_text.getText());
			fahrenheit_label.setText(((c*1.8)+32) + " Fahrenheit");
		}
	}));
frame.setLayout(new GridLayout(2, 2, 3, 3))
frame.add(temp_text)
frame.add(celsius_label)
frame.add(convert_button)
frame.add(fahrenheit_label)
frame.setSize(300, 80)
frame.setVisible(true)